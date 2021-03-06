package com.builtbroken.mc.fluids;

import com.builtbroken.mc.fluids.bucket.BucketMaterial;
import com.builtbroken.mc.fluids.bucket.BucketMaterialHandler;
import com.builtbroken.mc.fluids.bucket.ItemFluidBucket;
import com.builtbroken.mc.fluids.fluid.BlockMilk;
import com.builtbroken.mc.fluids.mods.BucketHandler;
import com.builtbroken.mc.fluids.mods.agricraft.AgricraftWaterPad;
import com.builtbroken.mc.fluids.mods.agricraft.AgricraftWaterPadFilled;
import com.builtbroken.mc.fluids.mods.pam.PamFreshWaterBucketRecipe;
import com.builtbroken.mc.fluids.mods.pam.PamMilkBucketRecipe;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.RecipeSorter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static net.minecraftforge.oredict.RecipeSorter.Category.SHAPED;

/**
 * Module class for handling all interaction with fluids for Voltz Engine and it's sub mods
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/2/2017.
 */
@Mod(modid = FluidModule.DOMAIN, name = "VoltzEngine Fluids module", version = FluidModule.VERSION)
public final class FluidModule
{
    public static final String DOMAIN = "vefluids";

    public static final String MAJOR_VERSION = "@MAJOR@";
    public static final String MINOR_VERSION = "@MINOR@";
    public static final String REVISION_VERSION = "@REVIS@";
    public static final String BUILD_VERSION = "@BUILD@";
    public static final String VERSION = MAJOR_VERSION + "." + MINOR_VERSION + "." + REVISION_VERSION + "." + BUILD_VERSION;

    /** Information output thing */
    public static final Logger logger = LogManager.getLogger("SBM-NoMoreRain");
    public static Configuration config;
    public static Configuration bucketConfig;

    @SidedProxy(clientSide = "com.builtbroken.mc.fluids.ClientProxy", serverSide = "com.builtbroken.mc.fluids.CommonProxy")
    public static CommonProxy proxy;

    //Internal settings
    public static boolean GENERATE_MILK_FLUID = true;

    //Load calls
    private static boolean loadBucket = false;

    //Content
    public static ItemFluidBucket bucket;

    public static Fluid fluid_milk;

    /**
     * Called to request that the bucket loads
     */
    public static void doLoadBucket()
    {
        loadBucket = true;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        config = new Configuration(new File(event.getModConfigurationDirectory(), "bbm/Fluid_Module/core.cfg"));
        config.load();

        bucketConfig = new Configuration(new File(config.getConfigFile().getParent(), "bucket_materials.cfg"));
        bucketConfig.load();

        if (bucketConfig.hasKey(Configuration.CATEGORY_GENERAL, "metaValues"))
        {
            String[] values = bucketConfig.getStringList("metaValues", Configuration.CATEGORY_GENERAL, new String[]{""}, "");
            if (values != null)
            {
                for (String s : values)
                {
                    if (s != null && s.contains(":"))
                    {
                        String[] split = s.split(":");
                        try
                        {
                            int meta = Integer.parseInt(split[1]);
                            BucketMaterialHandler.reserveMaterial(split[0], meta);
                        }
                        catch (NumberFormatException e)
                        {
                            throw new RuntimeException("Invalid data [" + s + "] in metaValue field in " + bucketConfig.getConfigFile());
                        }
                    }
                    else
                    {
                        throw new RuntimeException("Invalid data [" + s + "] in metaValue field in " + bucketConfig.getConfigFile());
                    }
                }
            }
        }

        GENERATE_MILK_FLUID = config.getBoolean("EnableMilkFluidGeneration", Configuration.CATEGORY_GENERAL, GENERATE_MILK_FLUID, "Will generate a fluid for milk allowing for the bucket to be used for gathering milk from cows");
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (loadBucket)
        {
            this.bucket = new ItemFluidBucket(DOMAIN + ":bucket");
            GameRegistry.registerItem(bucket, "veBucket", DOMAIN);
            MinecraftForge.EVENT_BUS.register(bucket);
        }

        if (Loader.isModLoaded("AgriCraft"))
        {
            BucketHandler.addBucketHandler(com.InfinityRaider.AgriCraft.init.Blocks.blockWaterPad, new AgricraftWaterPad());
            BucketHandler.addBucketHandler(com.InfinityRaider.AgriCraft.init.Blocks.blockWaterPadFull, new AgricraftWaterPadFilled());
        }
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        //Load milk fluid and block
        if (GENERATE_MILK_FLUID && FluidRegistry.getFluid("milk") == null)
        {
            fluid_milk = new Fluid("milk");
            FluidRegistry.registerFluid(fluid_milk);
            Block blockMilk = new BlockMilk(fluid_milk);
            GameRegistry.registerBlock(blockMilk, "veBlockMilk");
        }

        //Load per material configs
        for (BucketMaterial material : BucketMaterialHandler.getMaterials())
        {
            material.handleConfig(bucketConfig);
        }

        //Load recipe handling for other mods
        if (bucket != null)
        {
            //TODO add pam's harvest craft support
            if (Loader.isModLoaded("harvestcraft"))
            {
                if (config.getBoolean("EnableRegisteringMilkBucket", "PamHarvestCraftSupport", true, "Registers the milk bucket to the ore dictionary to be used in Pam's Harvest Craft recipes"))
                {
                    RecipeSorter.register(DOMAIN + ":woodenBucketFreshMilk", PamMilkBucketRecipe.class, SHAPED, "after:minecraft:shaped");
                    if (FluidRegistry.getFluid("milk") != null)
                    {
                        Item itemFreshMilk = (Item) Item.itemRegistry.getObject("harvestcraft:freshmilkItem");
                        if (itemFreshMilk == null)
                        {
                            logger.error("Failed to find item harvestcraft:freshmilkItem");
                        }

                        FluidStack milkFluidStack = new FluidStack(FluidRegistry.getFluid("milk"), FluidContainerRegistry.BUCKET_VOLUME);
                        for (BucketMaterial material : BucketMaterialHandler.getMaterials())
                        {
                            ItemStack milkBucket = new ItemStack(bucket, 1, material.metaValue);
                            bucket.fill(milkBucket, milkFluidStack, true);

                            GameRegistry.addRecipe(new PamMilkBucketRecipe(milkBucket, new ItemStack(itemFreshMilk, 4, 0)));
                        }
                    }
                }
                if (config.getBoolean("EnableRegisteringFreshWaterBucket", "PamHarvestCraftSupport", true, "Registers the water bucket to the ore dictionary to be used in Pam's Harvest Craft recipes"))
                {
                    RecipeSorter.register(DOMAIN + ":woodenBucketFreshMilk", PamFreshWaterBucketRecipe.class, SHAPED, "after:minecraft:shaped");
                    if (FluidRegistry.getFluid("milk") != null)
                    {
                        Item itemFreshWater = (Item) Item.itemRegistry.getObject("harvestcraft:freshwaterItem");
                        if (itemFreshWater == null)
                        {
                            logger.error("Failed to find item harvestcraft:freshwaterItem");
                        }

                        FluidStack waterStack = new FluidStack(FluidRegistry.WATER, FluidContainerRegistry.BUCKET_VOLUME);
                        for (BucketMaterial material : BucketMaterialHandler.getMaterials())
                        {
                            ItemStack milkBucket = new ItemStack(bucket, 1, material.metaValue);
                            bucket.fill(milkBucket, waterStack, true);

                            GameRegistry.addRecipe(new PamFreshWaterBucketRecipe(milkBucket, new ItemStack(itemFreshWater, 1, 0)));
                        }
                    }
                }
            }
        }
        proxy.postInit();
        config.save();
    }

    @Mod.EventHandler
    public void loadCompleted(FMLLoadCompleteEvent event)
    {
        //Get prop
        Property prop = bucketConfig.get(Configuration.CATEGORY_GENERAL, "metaValues", new String[]{""});
        prop.comment = "List of materials to meta values for containers. Do not change any of these values unless you know what you are doing. Changing these values will affect the world save and could result in unexpected behavior.";

        //Create list
        String[] list = new String[BucketMaterialHandler.getMaterials().size()];
        int i = 0;
        for (BucketMaterial material : BucketMaterialHandler.getMaterials())
        {
            list[i] = material.materialName + ":" + material.metaValue;
            i++;
        }
        //Set list
        prop.set(list);

        //Save config
        bucketConfig.save();
    }
}
