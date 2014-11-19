package com.worldcretornica.plotme_core;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import com.worldcretornica.plotme_core.api.*;
import com.worldcretornica.plotme_core.utils.Util;

import java.util.*;

public class PlotMeCoreManager {

    private final PlotMe_Core plugin;
    private HashSet<UUID> playersignoringwelimit;
    private HashMap<String, PlotMapInfo> plotmaps;

    public PlotMeCoreManager(PlotMe_Core instance) {
        plugin = instance;
        setPlayersIgnoringWELimit(new HashSet<UUID>());
        plotmaps = new HashMap<>();
    }

    public static int getIdX(String id) {
        return Integer.parseInt(id.substring(0, id.indexOf(";")));
    }

    public static int getIdZ(String id) {
        return Integer.parseInt(id.substring(id.indexOf(";") + 1));
    }

    public int getNbOwnedPlot(IPlayer player, IWorld world) {
        return getNbOwnedPlot(player.getUniqueId(), player.getName(), world.getName());
    }

    public int getNbOwnedPlot(UUID uuid, String name, String world) {
        return plugin.getSqlManager().getPlotCount(world, uuid, name);
    }

    private boolean isEconomyEnabled(String world) {
        if (plugin.getServerBridge().getConfig().getBoolean("globalUseEconomy") || plugin.getServerBridge().getEconomy() != null) {
            return false;
        }
        PlotMapInfo pmi = getMap(world.toLowerCase());

        if (pmi == null) {
            return false;
        } else {
            return pmi.isUseEconomy() && plugin.getServerBridge().getConfig().getBoolean("globalUseEconomy") && plugin.getServerBridge().getEconomy() != null;
        }
    }

    public boolean isEconomyEnabled(PlotMapInfo pmi) {
        if (plugin.getServerBridge().getConfig().getBoolean("globalUseEconomy") || plugin.getServerBridge().getEconomy() != null) {
            return false;
        }
        if (pmi == null) {
            return false;
        } else {
            return pmi.isUseEconomy() && plugin.getServerBridge().getConfig().getBoolean("globalUseEconomy") && plugin.getServerBridge().getEconomy() != null;
        }
    }

    public boolean isEconomyEnabled(IWorld world) {
        return isEconomyEnabled(world.getName());
    }

    public PlotMapInfo getMap(IWorld world) {
        if (world == null) {
            return null;
        } else {
            String worldname = world.getName().toLowerCase();
            return getMap(worldname);
        }
    }

    public PlotMapInfo getMap(String world) {
        if (plotmaps.containsKey(world)) {
            return plotmaps.get(world);
        } else {
            return null;
        }
    }

    public PlotMapInfo getMap(ILocation location) {
        String worldname = location.getWorld().getName().toLowerCase();
        return getMap(worldname);
    }

    public PlotMapInfo getMap(IPlayer player) {
        String worldname = player.getWorld().getName().toLowerCase();
        return getMap(worldname);
    }

    public Plot getPlotById(String id, IWorld world) {
        return getPlotById(id, world.getName());
    }

    public Plot getPlotById(String id, String name) {
        PlotMapInfo pmi = getMap(name.toLowerCase());

        if (pmi == null) {
            return null;
        }

        return pmi.getPlot(id);
    }

    public static Plot getPlotById(String id, PlotMapInfo pmi) {
        if (pmi == null) {
            return null;
        }

        return pmi.getPlot(id);
    }

    public Plot getPlotById(String id, IPlayer player) {
        PlotMapInfo pmi = getMap(player.getWorld());

        if (pmi == null) {
            return null;
        }

        return pmi.getPlot(id);
    }

    public Plot getPlotById(IPlayer player) {
        PlotMapInfo pmi = getMap(player);
        String id = getPlotId(player);

        if (pmi == null || id.isEmpty()) {
            return null;
        }

        return pmi.getPlot(id);
    }

    public static Plot getPlotById(IPlayer player, PlotMapInfo pmi) {
        String id = getPlotId(player);

        if (pmi == null || id.isEmpty()) {
            return null;
        }

        return pmi.getPlot(id);
    }

    public void removePlot(IWorld world, String id) {
        PlotMapInfo pmi = getMap(world);

        if (pmi == null) {
            return;
        }

        pmi.removePlot(id);
    }

    public static void removePlot(PlotMapInfo pmi, String id) {
        if (pmi == null) {
            return;
        }

        pmi.removePlot(id);
    }

    public void addPlot(IWorld world, String id, Plot plot) {
        PlotMapInfo pmi = getMap(world);

        if (pmi == null) {
            return;
        }

        pmi.addPlot(id, plot);
        plugin.getServerBridge().getEventFactory().callPlotLoadedEvent(plugin, world, plot);
    }

    public void addPlot(IWorld world, String id, Plot plot, PlotMapInfo pmi) {
        if (pmi == null) {
            return;
        }

        pmi.addPlot(id, plot);
        plugin.getServerBridge().getEventFactory().callPlotLoadedEvent(plugin, world, plot);
    }

    public IWorld getFirstWorld() {
        if (plotmaps != null) {
            if (plotmaps.keySet().toArray().length > 0) {
                return plugin.getServerBridge().getWorld((String) plotmaps.keySet().toArray()[0]);
            }
        }
        return null;
    }

    /**
     * Checks if world is a PlotWorld
     *
     * @param world world to be checked
     * @return true if world is plotworld, false otherwise
     */
    public boolean isPlotWorld(IWorld world) {
        if (world == null || getGenMan(world) == null) {
            return false;
        }
        return plotmaps.containsKey(world.getName().toLowerCase());
    }

    /**
     * Checks if location is a PlotWorld
     * @param location location to be checked
     * @return true if world is plotworld, false otherwise
     */
    public boolean isPlotWorld(ILocation location) {
        if (getGenMan(location.getWorld()) == null) {
            return false;
        } else {
            return plotmaps.containsKey(location.getWorld().getName().toLowerCase());
        }
    }

    public boolean isPlotWorld(IPlayer player) {
        if (getGenMan(player.getWorld()) == null) {
            return false;
        } else {
            return plotmaps.containsKey(player.getWorld().getName().toLowerCase());
        }
    }

    public boolean isPlotWorld(IBlock block) {
        if (getGenMan(block.getWorld()) != null)
            if (plotmaps.containsKey(block.getWorld().getName().toLowerCase())) {
                return true;
            }
        return false;
    }

    public Plot createPlot(IWorld world, String id, String owner, UUID uuid, PlotMapInfo pmi) {
        if (isPlotAvailable(id, pmi) && !id.isEmpty()) {
            Plot plot = new Plot(plugin, owner, uuid, world, id, pmi.getDaysToExpiration());

            setOwnerSign(world, plot);

            addPlot(world, id, plot, pmi);

            plugin.getSqlManager().addPlot(plot, getIdX(id), getIdZ(id), world);
            return plot;
        } else {
            return null;
        }
    }

    public boolean movePlot(IWorld world, String idFrom, String idTo) {

        if (!getGenMan(world).movePlot(world, idFrom, idTo)) {
            return false;
        }

        Plot plot1 = getPlotById(idFrom, world);
        Plot plot2 = getPlotById(idTo, world);

        if (plot1 != null) {
            if (plot2 != null) {
                int idX = getIdX(idTo);
                int idZ = getIdZ(idTo);
                plugin.getSqlManager().deletePlot(idX, idZ, world.getName());
                removePlot(world, idFrom);
                removePlot(world, idTo);
                idX = getIdX(idFrom);
                idZ = getIdZ(idFrom);
                plugin.getSqlManager().deletePlot(idX, idZ, world.getName());

                plot2.setId(idFrom);
                plugin.getSqlManager().addPlot(plot2, idX, idZ, topX(idFrom, world), bottomX(idFrom, world), topZ(idFrom, world), bottomZ(idFrom, world));
                addPlot(world, idFrom, plot2);

                HashMap<String, UUID> allowed = plot2.allowed().getAllPlayers();
                for (String player : allowed.keySet()) {
                    plugin.getSqlManager().addPlotAllowed(player, allowed.get(player), idX, idZ, world.getName());
                }

                HashMap<String, UUID> denied = plot2.denied().getAllPlayers();
                for (String player : denied.keySet()) {
                    plugin.getSqlManager().addPlotDenied(player, denied.get(player), idX, idZ, world.getName());
                }

                idX = getIdX(idTo);
                idZ = getIdZ(idTo);
                plot1.setId(idTo);
                plugin.getSqlManager().addPlot(plot1, idX, idZ, topX(idTo, world), bottomX(idTo, world), topZ(idTo, world), bottomZ(idTo, world));
                addPlot(world, idTo, plot1);

                allowed = plot1.allowed().getAllPlayers();
                for (String player : allowed.keySet()) {
                    plugin.getSqlManager().addPlotAllowed(player, allowed.get(player), idX, idZ, world.getName());
                }

                denied = plot1.denied().getAllPlayers();
                for (String player : denied.keySet()) {
                    plugin.getSqlManager().addPlotDenied(player, denied.get(player), idX, idZ, world.getName());
                }

                setOwnerSign(world, plot1);
                setSellSign(world, plot1);
                setOwnerSign(world, plot2);
                setSellSign(world, plot2);

            } else {
                int idX = getIdX(idFrom);
                int idZ = getIdZ(idFrom);
                plugin.getSqlManager().deletePlot(idX, idZ, world.getName());
                removePlot(world, idFrom);
                idX = getIdX(idTo);
                idZ = getIdZ(idTo);
                plot1.setId(idTo);
                plugin.getSqlManager().addPlot(plot1, idX, idZ, topX(idTo, world), bottomX(idTo, world), topZ(idTo, world), bottomZ(idTo, world));
                addPlot(world, idTo, plot1);

                HashMap<String, UUID> allowed = plot1.allowed().getAllPlayers();
                for (String player : allowed.keySet()) {
                    plugin.getSqlManager().addPlotAllowed(player, allowed.get(player), idX, idZ, world.getName());
                }

                HashMap<String, UUID> denied = plot1.denied().getAllPlayers();
                for (String player : denied.keySet()) {
                    plugin.getSqlManager().addPlotDenied(player, denied.get(player), idX, idZ, world.getName());
                }

                setOwnerSign(world, plot1);
                setSellSign(world, plot1);
                removeOwnerSign(world, idFrom);
                getGenMan(world).removeSellerDisplay(world, idFrom);

            }
        } else if (plot2 != null) {
            int idX = getIdX(idTo);
            int idZ = getIdZ(idTo);
            plugin.getSqlManager().deletePlot(idX, idZ, world.getName());
            removePlot(world, idTo);

            idX = getIdX(idFrom);
            idZ = getIdZ(idFrom);
            plot2.setId(idFrom);
            plugin.getSqlManager().addPlot(plot2, idX, idZ, topX(idFrom, world), bottomX(idFrom, world), topZ(idFrom, world), bottomZ(idFrom, world));
            addPlot(world, idFrom, plot2);

            HashMap<String, UUID> allowed = plot2.allowed().getAllPlayers();
            for (String player : allowed.keySet()) {
                plugin.getSqlManager().addPlotAllowed(player, allowed.get(player), idX, idZ, world.getName());
            }

            HashMap<String, UUID> denied = plot2.denied().getAllPlayers();
            for (String player : denied.keySet()) {
                plugin.getSqlManager().addPlotDenied(player, denied.get(player), idX, idZ, world.getName());
            }

            setOwnerSign(world, plot2);
            setSellSign(world, plot2);
            removeOwnerSign(world, idTo);
            getGenMan(world).removeSellerDisplay(world, idTo);
        }

        return true;
    }

    public void removeLWC(IWorld world, String id) {
        ILocation bottom = getGenMan(world).getBottom(world, id);
        ILocation top = getGenMan(world).getTop(world, id);
        final int x1 = bottom.getBlockX();
        final int y1 = bottom.getBlockY();
        final int z1 = bottom.getBlockZ();
        final int x2 = top.getBlockX();
        final int y2 = top.getBlockY();
        final int z2 = top.getBlockZ();
        final String wname = world.getName();

        plugin.getServerBridge().runTaskAsynchronously(new Runnable() {
            @Override
            public void run() {
                LWC lwc = LWC.getInstance();
                List<Protection> protections = lwc.getPhysicalDatabase().loadProtections(wname, x1, x2, y1, y2, z1, z2);

                for (Protection protection : protections) {
                    protection.remove();
                }
            }
        });
    }

    public static void setOwnerSign(IWorld world, Plot plot) {
        String id = plot.getId();
        String line1 = "ID: " + id;
        String line2 = "";
        String line3 = plot.getOwner();
        String line4 = "";
        getGenMan(world).setOwnerDisplay(world, plot.getId(), line1, line2, line3, line4);
    }

    public void setSellSign(IWorld world, Plot plot) {
        String id = plot.getId();

        getGenMan(world).removeSellerDisplay(world, id);

        if (plot.isForSale() || plot.isAuctioned()) {
            String line1 = "";
            String line2 = "";
            String line3 = "";
            String line4 = "";
            if (plot.isForSale()) {
                line1 = Util().C("SignForSale");
                line2 = Util().C("SignPrice");
                if (plot.getCustomPrice() % 1 == 0) {
                    line3 = Util().C("SignPriceColor") + Math.round(plot.getCustomPrice());
                } else {
                    line3 = Util().C("SignPriceColor") + plot.getCustomPrice();
                }
                line4 = "/plotme " + Util().C("CommandBuy");
            }

            getGenMan(world).setSellerDisplay(world, plot.getId(), line1, line2, line3, line4);

            line1 = "";
            line2 = "";
            line3 = "";
            line4 = "";

            if (plot.isAuctioned()) {
                line1 = Util().C("SignOnAuction");
                if (plot.getCurrentBidder().isEmpty()) {
                    line2 = Util().C("SignMinimumBid");
                } else {
                    line2 = Util().C("SignCurrentBid");
                }
                if (plot.getCurrentBid() % 1 == 0) {
                    line3 = Util().C("SignCurrentBidColor") + Math.round(plot.getCurrentBid());
                } else {
                    line3 = Util().C("SignCurrentBidColor") + plot.getCurrentBid();
                }
                line4 = "/plotme " + Util().C("CommandBid") + " <x>";
            }

            getGenMan(world).setAuctionDisplay(world, plot.getId(), line1, line2, line3, line4);
        }
    }

    public static void adjustLinkedPlots(String id, IWorld world) {
        //TODO
        Map<String, Plot> plots = new HashMap<>(); //getPlots(world);

        IPlotMe_GeneratorManager gm = getGenMan(world);

        int x = getIdX(id);
        int z = getIdZ(id);

        Plot p11 = plots.get(id);

        if (p11 != null) {
            Plot p01 = plots.get((x - 1) + ";" + z);
            Plot p10 = plots.get(x + ";" + (z - 1));
            Plot p12 = plots.get(x + ";" + (z + 1));
            Plot p21 = plots.get((x + 1) + ";" + z);
            Plot p00 = plots.get((x - 1) + ";" + (z - 1));
            Plot p02 = plots.get((x - 1) + ";" + (z + 1));
            Plot p20 = plots.get((x + 1) + ";" + (z - 1));
            Plot p22 = plots.get((x + 1) + ";" + (z + 1));

            if (p01 != null && p01.getOwner().equalsIgnoreCase(p11.getOwner())) {
                gm.fillroad(p01.getId(), p11.getId(), world);
            }

            if (p10 != null && p10.getOwner().equalsIgnoreCase(p11.getOwner())) {
                gm.fillroad(p10.getId(), p11.getId(), world);
            }

            if (p12 != null && p12.getOwner().equalsIgnoreCase(p11.getOwner())) {
                gm.fillroad(p12.getId(), p11.getId(), world);
            }

            if (p21 != null && p21.getOwner().equalsIgnoreCase(p11.getOwner())) {
                gm.fillroad(p21.getId(), p11.getId(), world);
            }

            if (p00 != null && p10 != null && p01 != null
                        && p00.getOwner().equalsIgnoreCase(p11.getOwner())
                        && p11.getOwner().equalsIgnoreCase(p10.getOwner())
                        && p10.getOwner().equalsIgnoreCase(p01.getOwner())) {
                gm.fillmiddleroad(p00.getId(), p11.getId(), world);
            }

            if (p10 != null && p20 != null && p21 != null
                        && p10.getOwner().equalsIgnoreCase(p11.getOwner())
                        && p11.getOwner().equalsIgnoreCase(p20.getOwner())
                        && p20.getOwner().equalsIgnoreCase(p21.getOwner())) {
                gm.fillmiddleroad(p20.getId(), p11.getId(), world);
            }

            if (p01 != null && p02 != null && p12 != null
                        && p01.getOwner().equalsIgnoreCase(p11.getOwner())
                        && p11.getOwner().equalsIgnoreCase(p02.getOwner())
                        && p02.getOwner().equalsIgnoreCase(p12.getOwner())) {
                gm.fillmiddleroad(p02.getId(), p11.getId(), world);
            }

            if (p12 != null && p21 != null && p22 != null
                        && p12.getOwner().equalsIgnoreCase(p11.getOwner())
                        && p11.getOwner().equalsIgnoreCase(p21.getOwner())
                        && p21.getOwner().equalsIgnoreCase(p22.getOwner())) {
                gm.fillmiddleroad(p22.getId(), p11.getId(), world);
            }

        }
    }

    public void clear(IWorld iWorld, Plot plot, ICommandSender sender, ClearReason reason) {
        String id = plot.getId();

        plot.setForSale(false);
        plot.setProtect(false);
        plot.setAuctioned(false);
        plot.setAuctionedDate(null);
        plot.setCurrentBid(0);
        plot.setCurrentBidder("");

        String world = iWorld.getName();
        int idX = getIdX(id);
        int idZ = getIdZ(id);

        SqlManager sm = plugin.getSqlManager();

        sm.updatePlot(idX, idZ, world, "forsale", false);
        sm.updatePlot(idX, idZ, world, "protected", false);
        sm.updatePlot(idX, idZ, world, "auctionned", false);
        sm.updatePlot(idX, idZ, world, "auctionneddate", "");
        sm.updatePlot(idX, idZ, world, "currentbid", 0);
        sm.updatePlot(idX, idZ, world, "currentbidder", "");

        if (getMap(iWorld).isUseProgressiveClear()) {
            plugin.addPlotToClear(new PlotToClear(world, id, sender, reason));
        } else {
            getGenMan(iWorld).clear(iWorld, id);
            if (plugin.getServerBridge().getUsinglwc()) {
                removeLWC(iWorld, id);
            }
            sender.sendMessage(Util().C("MsgPlotCleared"));
        }
    }

    public boolean isPlotAvailable(String id, IWorld world) {
        return isPlotAvailable(id, world.getName().toLowerCase());
    }

    public boolean isPlotAvailable(String id, IPlayer player) {
        return isPlotAvailable(id, player.getWorld());
    }

    private boolean isPlotAvailable(String id, String world) {
        PlotMapInfo pmi = getMap(world);

        return pmi != null && pmi.getPlot(id) == null;
    }

    public static boolean isPlotAvailable(String id, PlotMapInfo pmi) {

        return pmi != null && pmi.getPlot(id) == null;
    }

    public static String getPlotId(ILocation location) {
        if (getGenMan(location.getWorld()) == null) {
            return "";
        }

        IPlotMe_GeneratorManager gen = getGenMan(location.getWorld());

        if (gen == null) {
            return "";
        } else {
            return gen.getPlotId(location);
        }
    }

    public static String getPlotId(IPlayer player) {
        if (getGenMan(player.getWorld()) == null) {
            return "";
        }

        IPlotMe_GeneratorManager gen = getGenMan(player.getWorld());

        if (gen == null) {
            return "";
        } else {
            return gen.getPlotId(player);
        }
    }

    private static IPlotMe_GeneratorManager getGenMan(IWorld world) {
        return PlotMe_Core.getGenManager(world);
    }

    public IPlotMe_GeneratorManager getGenMan(String name) {
        return plugin.getGenManager(name);
    }

    public static ILocation getPlotBottomLoc(IWorld world, String id) {
        return getGenMan(world).getPlotBottomLoc(world, id);
    }

    public static ILocation getPlotTopLoc(IWorld world, String id) {
        return getGenMan(world).getPlotTopLoc(world, id);
    }

    public void adjustWall(IPlayer player) {
        IWorld world = player.getWorld();
        String id = getPlotId(player);
        Plot plot = getPlotById(id, world);

        getGenMan(world).adjustPlotFor(world, id, true, plot.isProtect(), plot.isAuctioned(), plot.isForSale());
    }

    public static void removeOwnerSign(IWorld world, String id) {
        getGenMan(world).removeOwnerDisplay(world, id);
    }

    public static void removeSellSign(IWorld world, String id) {
        getGenMan(world).removeSellerDisplay(world, id);
    }

    public static void removeAuctionSign(IWorld world, String id) {
        getGenMan(world).removeAuctionDisplay(world, id);
    }

    public static boolean isValidId(IWorld world, String id) {
        return getGenMan(world).isValidId(id);
    }

    public static int bottomX(String id, IWorld world) {
        return getGenMan(world).bottomX(id, world);
    }

    public static int topX(String id, IWorld world) {
        return getGenMan(world).topX(id, world);
    }

    public static int bottomZ(String id, IWorld world) {
        return getGenMan(world).bottomZ(id, world);
    }

    public static int topZ(String id, IWorld world) {
        return getGenMan(world).topZ(id, world);
    }

    public void setBiome(IWorld world, String id, IBiome biome) {
        getGenMan(world).setBiome(world, id, biome);
        plugin.getSqlManager().updatePlot(getIdX(id), getIdZ(id), world.getName(), "biome", biome.name());
    }

    public static ILocation getPlotHome(IWorld world, String id) {
        return getGenMan(world).getPlotHome(world, id);
    }

    public static List<IPlayer> getPlayersInPlot(IWorld world, String id) {
        return getGenMan(world).getPlayersInPlot(world, id);
    }

    public HashSet<UUID> getPlayersIgnoringWELimit() {
        return playersignoringwelimit;
    }

    public void setPlayersIgnoringWELimit(HashSet<UUID> playersignoringwelimit) {
        this.playersignoringwelimit = playersignoringwelimit;
    }

    public void addPlayerIgnoringWELimit(UUID uuid) {
        playersignoringwelimit.add(uuid);
    }

    public void removePlayerIgnoringWELimit(UUID uuid) {
        playersignoringwelimit.remove(uuid);
    }

    public boolean isPlayerIgnoringWELimit(UUID uuid) {
        return playersignoringwelimit.contains(uuid);
    }

    public Map<String, PlotMapInfo> getPlotMaps() {
        return plotmaps;
    }

    public void addPlotMap(String world, PlotMapInfo map) {
        plotmaps.put(world, map);
    }

    private Util Util() {
        return plugin.getUtil();
    }
}
