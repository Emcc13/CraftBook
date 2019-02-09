package com.sk89q.craftbook.mechanics.ic.gates.world.blocks;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.bukkit.CraftBookPlugin;
import com.sk89q.craftbook.bukkit.util.CraftBookBukkitUtil;
import com.sk89q.craftbook.mechanics.ic.AbstractICFactory;
import com.sk89q.craftbook.mechanics.ic.ConfigurableIC;
import com.sk89q.craftbook.mechanics.ic.AbstractSelfTriggeredIC;
import com.sk89q.craftbook.mechanics.ic.ChipState;
import com.sk89q.craftbook.mechanics.ic.IC;
import com.sk89q.craftbook.mechanics.ic.ICFactory;
import com.sk89q.craftbook.mechanics.ic.ICVerificationException;
import com.sk89q.craftbook.util.BlockUtil;
import com.sk89q.craftbook.util.BlockSyntax;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.craftbook.util.ICUtil;
import com.sk89q.craftbook.util.SearchArea;
import com.sk89q.craftbook.util.BlockSyntax;
import com.sk89q.craftbook.util.RegexUtil;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import java.util.Set;
import java.util.HashSet;

public class CombineHarvester extends AbstractSelfTriggeredIC {

    public CombineHarvester(Server server, ChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    SearchArea area;

    @Override
    public void load() {

        area = SearchArea.createArea(getLocation().getBlock(), getLine(2));
    }

    @Override
    public String getTitle() {

        return "Combine Harvester";
    }

    @Override
    public String getSignTitle() {

        return "HARVEST";
    }

    @Override
    public void trigger(ChipState chip) {

        if (chip.getInput(0)) chip.setOutput(0, harvest());
    }

    @Override
    public void think(ChipState chip) {

        if(chip.getInput(0)) return;

        for(int i = 0; i < 10; i++)
            chip.setOutput(0, harvest());
    }

    public boolean harvest() {

        Block b = area.getRandomBlockInArea();

        if(b == null) return false;

        if (harvestable(b)) {
            ICUtil.collectItem(this, BlockVector3.at(0, 1, 0), BlockUtil.getBlockDrops(b, null));
            b.setType(Material.AIR);
            return true;
        }
        return false;
    }

    public boolean harvestable(Block block) {
        Material above = block.getRelative(0, 1, 0).getType();
        Material below = block.getRelative(0, -1, 0).getType();
        Material blockMaterial = block.getType();
        if (((Factory) getFactory()).blockBlacklist.contains(blockMaterial)){
            return false;
        }
        switch (blockMaterial) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case NETHER_WART_BLOCK:
            case COCOA:
                Ageable ageable = (Ageable) block.getBlockData();
                return ageable.getAge() == ageable.getMaximumAge();
            case CACTUS:
                return below == Material.CACTUS && above != Material.CACTUS;
            case SUGAR_CANE:
                return below == Material.SUGAR_CANE && above != Material.SUGAR_CANE;
            case VINE:
                return above == Material.VINE && below != Material.VINE;
            case MELON:
            case PUMPKIN:
                return true;
            default:
                return Tag.LOGS.isTagged(blockMaterial);
        }
    }

    public static class Factory extends AbstractICFactory implements ConfigurableIC {

        public Set<Material> blockBlacklist;
        private static ParserContext BLOCK_CONTEXT = new ParserContext();
        static {
            BLOCK_CONTEXT.setPreferringWildcard(false);
            BLOCK_CONTEXT.setRestricted(false);
        }

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new CombineHarvester(getServer(), sign, this);
        }

        @Override
        public String getShortDescription() {

            return "Harvests nearby crops.";
        }

        @Override
        public String[] getLineHelp() {

            return new String[] {"SearchArea", null};
        }

        @Override
        public void verify(ChangedSign sign) throws ICVerificationException {
            if(!SearchArea.isValidArea(CraftBookBukkitUtil.toSign(sign).getBlock(), sign.getLine(2)))
                throw new ICVerificationException("Invalid SearchArea on 3rd line!");
        }

        private static Material getMaterial(String line) {
            if (line == null || line.trim().isEmpty()) {
                return null;
            }
            Material material = null;
            String[] dataSplit = RegexUtil.COLON_PATTERN.split(line.replace("\\:", ":"), 2);
            material = Material.getMaterial(dataSplit[0]);
            if (material == null) {
                CraftBookPlugin.logger().warning("Invalid block format: " + line);
            }
            return material;
        }

        @Override
        public void addConfiguration(YAMLProcessor config, String path){
            config.setComment(path+"blacklist", "Stops the IC from harvesting the listed blocks.");

            blockBlacklist = new HashSet<Material>();
            for (String possibleMaterial : config.getStringList(path+"blacklist",Lists.newArrayList())){
                Material mat = Factory.getMaterial(possibleMaterial);
                if (mat != null){
                    blockBlacklist.add(mat);
                }
            }
        }
    }
}