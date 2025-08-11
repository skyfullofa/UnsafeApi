package com.overlord.unsafe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.FoodStats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.FOOD;
import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.HEALTH;

@Mod(modid = UnsafeMod.MODID, name = UnsafeMod.NAME, version = UnsafeMod.VERSION)
public class UnsafeMod
{
    
    public static final String MODID = "examplemod";
    public static final String NAME = "Example Mod";
    public static final String VERSION = "1.0";

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
        logger = event.getModLog();
    }
    @SubscribeEvent
    public void onItemRegister(RegistryEvent.Register<Item> event){
        event.getRegistry().register(new UnsafeItem());
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // some example code
        logger.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }
    public static class UnsafeGuiInGame extends GuiIngameForge {
        public UnsafeGuiInGame(Minecraft mc) {
            super(mc);
        }

        @Override
        public void renderFood(int width, int height) {
            Method method;
            try {
                method = GuiIngameForge.class.getDeclaredMethod("pre", RenderGameOverlayEvent.ElementType.class);
                method.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            boolean pre;
            try {
                pre = (boolean) method.invoke(new GuiIngameForge(Minecraft.getMinecraft()),FOOD);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            if (pre) return;
            mc.mcProfiler.startSection("food");
            EntityPlayer player = (EntityPlayer)this.mc.getRenderViewEntity();
            GlStateManager.enableBlend();
            int left = width / 2 + 91;
            int top = height - right_height;
            right_height += 10;
            boolean unused = false;// Unused flag in vanilla, seems to be part of a 'fade out' mechanic

            FoodStats stats = mc.player.getFoodStats();
            int level = 0;

            for (int i = 0; i < 10; ++i)
            {
                int idx = i * 2 + 1;
                int x = left - i * 8 - 9;
                int y = top;
                int icon = 16;
                byte background = 0;

                if (mc.player.isPotionActive(MobEffects.HUNGER))
                {
                    icon += 36;
                    background = 13;
                }
                if (unused) background = 1; //Probably should be a += 1 but vanilla never uses this

                if (player.getFoodStats().getSaturationLevel() <= 0.0F && updateCounter % (level * 3 + 1) == 0)
                {
                    y = top + (rand.nextInt(3) - 1);
                }

                drawTexturedModalRect(x, y, 16 + background * 9, 27, 9, 9);

                if (idx < level)
                    drawTexturedModalRect(x, y, icon + 36, 27, 9, 9);
                else if (idx == level)
                    drawTexturedModalRect(x, y, icon + 45, 27, 9, 9);
            }
            GlStateManager.disableBlend();
            mc.mcProfiler.endSection();
            Method method1;
            try {
                method1 = GuiIngameForge.class.getDeclaredMethod("post", RenderGameOverlayEvent.ElementType.class);
                method1.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            try {
                method1.invoke(new GuiIngameForge(Minecraft.getMinecraft()),FOOD);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void renderHealth(int width, int height)
        {
            mc.getTextureManager().bindTexture(ICONS);
            Method method;
            try {
                method = GuiIngameForge.class.getDeclaredMethod("pre", RenderGameOverlayEvent.ElementType.class);
                method.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            boolean pre;
            try {
                pre = (boolean) method.invoke(new GuiIngameForge(Minecraft.getMinecraft()),HEALTH);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            if (pre) return;
            mc.mcProfiler.startSection("health");
            GlStateManager.enableBlend();

            EntityPlayer player = (EntityPlayer)this.mc.getRenderViewEntity();
            int health = 0;
            boolean highlight = healthUpdateCounter > (long)updateCounter && (healthUpdateCounter - (long)updateCounter) / 3L %2L == 1L;

            if (health < this.playerHealth && player.hurtResistantTime > 0)
            {
                this.lastSystemTime = Minecraft.getSystemTime();
                this.healthUpdateCounter = (long)(this.updateCounter + 20);
            }
            else if (health > this.playerHealth && player.hurtResistantTime > 0)
            {
                this.lastSystemTime = Minecraft.getSystemTime();
                this.healthUpdateCounter = (long)(this.updateCounter + 10);
            }

            if (Minecraft.getSystemTime() - this.lastSystemTime > 1000L)
            {
                this.playerHealth = health;
                this.lastPlayerHealth = health;
                this.lastSystemTime = Minecraft.getSystemTime();
            }

            this.playerHealth = health;
            int healthLast = this.lastPlayerHealth;

            IAttributeInstance attrMaxHealth = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
            float healthMax = (float)attrMaxHealth.getAttributeValue();
            float absorb = MathHelper.ceil(player.getAbsorptionAmount());

            int healthRows = MathHelper.ceil((healthMax + absorb) / 2.0F / 10.0F);
            int rowHeight = Math.max(10 - (healthRows - 2), 3);

            this.rand.setSeed((long)(updateCounter * 312871));

            int left = width / 2 - 91;
            int top = height - left_height;
            left_height += (healthRows * rowHeight);
            if (rowHeight != 10) left_height += 10 - rowHeight;

            int regen = -1;
            if (player.isPotionActive(MobEffects.REGENERATION))
            {
                regen = updateCounter % 25;
            }

            final int TOP =  9 * (mc.world.getWorldInfo().isHardcoreModeEnabled() ? 5 : 0);
            final int BACKGROUND = (highlight ? 25 : 16);
            int MARGIN = 16;
            if (player.isPotionActive(MobEffects.POISON))      MARGIN += 36;
            else if (player.isPotionActive(MobEffects.WITHER)) MARGIN += 72;
            float absorbRemaining = absorb;

            for (int i = MathHelper.ceil((healthMax + absorb) / 2.0F) - 1; i >= 0; --i)
            {
                //int b0 = (highlight ? 1 : 0);
                int row = MathHelper.ceil((float)(i + 1) / 10.0F) - 1;
                int x = left + i % 10 * 8;
                int y = top - row * rowHeight;

                if (health <= 4) y += rand.nextInt(2);
                if (i == regen) y -= 2;

                drawTexturedModalRect(x, y, BACKGROUND, TOP, 9, 9);

                if (highlight)
                {
                    if (i * 2 + 1 < healthLast)
                        drawTexturedModalRect(x, y, MARGIN + 54, TOP, 9, 9); //6
                    else if (i * 2 + 1 == healthLast)
                        drawTexturedModalRect(x, y, MARGIN + 63, TOP, 9, 9); //7
                }

                if (absorbRemaining > 0.0F)
                {
                    if (absorbRemaining == absorb && absorb % 2.0F == 1.0F)
                    {
                        drawTexturedModalRect(x, y, MARGIN + 153, TOP, 9, 9); //17
                        absorbRemaining -= 1.0F;
                    }
                    else
                    {
                        drawTexturedModalRect(x, y, MARGIN + 144, TOP, 9, 9); //16
                        absorbRemaining -= 2.0F;
                    }
                }
                else
                {
                    if (i * 2 + 1 < health)
                        drawTexturedModalRect(x, y, MARGIN + 36, TOP, 9, 9); //4
                    else if (i * 2 + 1 == health)
                        drawTexturedModalRect(x, y, MARGIN + 45, TOP, 9, 9); //5
                }
            }

            GlStateManager.disableBlend();
            mc.mcProfiler.endSection();
            Method method1;
            try {
                method1 = GuiIngameForge.class.getDeclaredMethod("post", RenderGameOverlayEvent.ElementType.class);
                method1.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            try {
                method1.invoke(new GuiIngameForge(Minecraft.getMinecraft()),HEALTH);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
