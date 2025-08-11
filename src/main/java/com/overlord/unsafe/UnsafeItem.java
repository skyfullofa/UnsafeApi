package com.overlord.unsafe;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class UnsafeItem extends Item implements Opcodes {
    private static final Unsafe unsafe;
    private static final Class<?> fakeGuiClass;
    public static final Object fakeGuiInstance;

    static {
        try {
            // 1. 获取Unsafe实例
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);

            // 2. 获取原始类字节码
            byte[] classBytes = Launch.classLoader.getClassBytes("com.overlord.unsafe.UnsafeItem$UnsafeGui");

            // 3. 使用ASM修改字节码
            ClassReader cr = new ClassReader(classBytes);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);

            cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
                @Override
                public void visit(int version, int access, String name, String signature,
                                  String superName, String[] interfaces) {
                    // 修改为GuiChat的类结构
                    super.visit(version, access,
                            "net/minecraft/client/gui/GuiChat",
                            signature,
                            "net/minecraft/client/gui/GuiScreen",
                            new String[]{"net/minecraft/client/gui/GuiYesNoCallback"});
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc,
                                                 String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name,
                                                    String desc, boolean itf) {
                            // 替换所有方法调用中的类名引用
                            if (owner.equals("com/overlord/unsafe/UnsafeItem$UnsafeGui")) {
                                owner = "net/minecraft/client/gui/GuiChat";
                            }
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name,
                                                   String desc) {
                            // 替换所有字段引用中的类名
                            if (owner.equals("com/overlord/unsafe/UnsafeItem$UnsafeGui")) {
                                owner = "net/minecraft/client/gui/GuiChat";
                            }
                            super.visitFieldInsn(opcode, owner, name, desc);
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            // 替换类型指令中的类名
                            if (type.equals("com/overlord/unsafe/UnsafeItem$UnsafeGui")) {
                                type = "net/minecraft/client/gui/GuiChat";
                            }
                            super.visitTypeInsn(opcode, type);
                        }
                    };
                }
            }, 0);

            // 4. 定义新类
            byte[] modifiedBytes = cw.toByteArray();
            fakeGuiClass = unsafe.defineAnonymousClass(GuiScreen.class, modifiedBytes, null);
            Logger logger = LogManager.getLogger();
            logger.info("Defining Anonymous Class : " + fakeGuiClass.getName());
            Field field = Class.class.getDeclaredField("name");
            field.setAccessible(true);
            field.set(fakeGuiClass, "net.minecraft.client.gui.GuiChat");
            fakeGuiInstance = fakeGuiClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create fake GuiChat", e);
        }
    }

    // 替换GUI实例的方法
    /*public static void setAFieldToB(Class<?> clazz, Object object, Object o) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isFinal(field.getModifiers()) && !field.getDeclaringClass()) {
                field.setAccessible(true);
                try {
                    field.set(o, field.get(object));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }*/
    public UnsafeItem(){
        setCreativeTab(CreativeTabs.COMBAT);
        setRegistryName("unsafe_item");
        setUnlocalizedName("unsafe_item");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        System.out.println(fakeGuiInstance);
        Minecraft.getMinecraft().displayGuiScreen((GuiScreen) fakeGuiInstance);
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }
    public static class UnsafeGui extends GuiScreen implements GuiYesNoCallback{
        private int enableButtonsTimer;
        private final ITextComponent causeOfDeath;

        public UnsafeGui()
        {
            this.causeOfDeath = new TextComponentString("Being Cheated");
        }

        public void initGui()
        {
            this.buttonList.clear();
            this.enableButtonsTimer = 0;

            if (this.mc.world.getWorldInfo().isHardcoreModeEnabled())
            {
                this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 72, I18n.format("deathScreen.spectate")));
                this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 96, I18n.format("deathScreen." + (this.mc.isIntegratedServerRunning() ? "deleteWorld" : "leaveServer"))));
            }
            else
            {
                this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 72, I18n.format("deathScreen.respawn")));
                this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 96, I18n.format("deathScreen.titleScreen")));

                if (this.mc.getSession() == null)
                {
                    (this.buttonList.get(1)).enabled = false;
                }
            }

            for (GuiButton guibutton : this.buttonList)
            {
                guibutton.enabled = false;
            }
        }

        protected void keyTyped(char typedChar, int keyCode) throws IOException
        {
        }

        protected void actionPerformed(GuiButton button) throws IOException
        {
            switch (button.id)
            {
                case 0:
                    this.mc.player.respawnPlayer();
                    this.mc.displayGuiScreen((GuiScreen)null);
                    break;
                case 1:

                    if (this.mc.world.getWorldInfo().isHardcoreModeEnabled())
                    {
                        this.mc.displayGuiScreen(new GuiMainMenu());
                    }
                    else
                    {
                        GuiYesNo guiyesno = new GuiYesNo(this, I18n.format("deathScreen.quit.confirm"), "", I18n.format("deathScreen.titleScreen"), I18n.format("deathScreen.respawn"), 0);
                        this.mc.displayGuiScreen(guiyesno);
                        guiyesno.setButtonDelay(20);
                    }
            }
        }

        public void confirmClicked(boolean result, int id)
        {
            if (result)
            {
                if (this.mc.world != null)
                {
                    this.mc.world.sendQuittingDisconnectingPacket();
                }

                this.mc.loadWorld((WorldClient)null);
                this.mc.displayGuiScreen(new GuiMainMenu());
            }
            else
            {
                this.mc.player.respawnPlayer();
                this.mc.displayGuiScreen((GuiScreen)null);
            }
        }

        public void drawScreen(int mouseX, int mouseY, float partialTicks)
        {
            boolean flag = this.mc.world.getWorldInfo().isHardcoreModeEnabled();

            this.drawGradientRect(0, 0, this.width, this.height, 1615855616, -1602211792);
            GlStateManager.pushMatrix();
            GlStateManager.scale(2.0F, 2.0F, 2.0F);
            this.drawCenteredString(this.fontRenderer, I18n.format(flag ? "deathScreen.title.hardcore" : "deathScreen.title"), this.width / 2 / 2, 30, 16777215);
            GlStateManager.popMatrix();

            if (this.causeOfDeath != null)
            {
                this.drawCenteredString(this.fontRenderer, this.causeOfDeath.getFormattedText(), this.width / 2, 85, 16777215);
            }

            this.drawCenteredString(this.fontRenderer, I18n.format("deathScreen.score") + ": " + TextFormatting.YELLOW + this.mc.player.getScore(), this.width / 2, 100, 16777215);

            if (this.causeOfDeath != null && mouseY > 85 && mouseY < 85 + this.fontRenderer.FONT_HEIGHT)
            {
                ITextComponent itextcomponent = this.getClickedComponentAt(mouseX);

                if (itextcomponent != null && itextcomponent.getStyle().getHoverEvent() != null)
                {
                    this.handleComponentHover(itextcomponent, mouseX, mouseY);
                }
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Nullable
        public ITextComponent getClickedComponentAt(int p_184870_1_)
        {
            if (this.causeOfDeath == null)
            {
                return null;
            }
            else
            {
                int i = this.mc.fontRenderer.getStringWidth(this.causeOfDeath.getFormattedText());
                int j = this.width / 2 - i / 2;
                int k = this.width / 2 + i / 2;
                int l = j;

                if (p_184870_1_ >= j && p_184870_1_ <= k)
                {
                    for (ITextComponent itextcomponent : this.causeOfDeath)
                    {
                        l += this.mc.fontRenderer.getStringWidth(GuiUtilRenderComponents.removeTextColorsIfConfigured(itextcomponent.getUnformattedComponentText(), false));

                        if (l > p_184870_1_)
                        {
                            return itextcomponent;
                        }
                    }

                    return null;
                }
                else
                {
                    return null;
                }
            }
        }

        public boolean doesGuiPauseGame()
        {
            return false;
        }

        public void updateScreen()
        {
            super.updateScreen();
            ++this.enableButtonsTimer;

            if (this.enableButtonsTimer == 20)
            {
                for (GuiButton guibutton : this.buttonList)
                {
                    guibutton.enabled = true;
                }
            }
        }
    }
}
