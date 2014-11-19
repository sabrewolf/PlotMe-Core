package com.worldcretornica.plotme_core.commands;

import com.worldcretornica.plotme_core.*;
import com.worldcretornica.plotme_core.api.IOfflinePlayer;
import com.worldcretornica.plotme_core.api.IPlayer;
import com.worldcretornica.plotme_core.api.IWorld;
import com.worldcretornica.plotme_core.api.event.InternalPlotResetEvent;
import net.milkbowl.vault.economy.EconomyResponse;

public class CmdReset extends PlotCommand {

    public CmdReset(PlotMe_Core instance) {
        super(instance);
    }

    public boolean exec(IPlayer player) {
        if (player.hasPermission("PlotMe.admin.reset") || player.hasPermission("PlotMe.use.reset")) {
            IWorld world = player.getWorld();
            PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(world);
            if (plugin.getPlotMeCoreManager().isPlotWorld(world)) {
                Plot plot = PlotMeCoreManager.getPlotById(player, pmi);

                if (plot == null) {
                    player.sendMessage("§c" + C("MsgNoPlotFound"));
                } else if (plot.isProtect()) {
                    player.sendMessage("§c" + C("MsgPlotProtectedCannotReset"));
                } else {
                    String playername = player.getName();
                    String id = plot.getId();

                    if (plot.getOwner().equalsIgnoreCase(playername) || player.hasPermission("PlotMe.admin.reset")) {

                        InternalPlotResetEvent event = sob.getEventFactory().callPlotResetEvent(plugin, world, plot, player);

                        if (!event.isCancelled()) {
                            plugin.getPlotMeCoreManager().setBiome(world, id, sob.getBiome("PLAINS"));
                            plugin.getPlotMeCoreManager().clear(world, plot, player, ClearReason.Reset);

                            if (plugin.getPlotMeCoreManager().isEconomyEnabled(pmi)) {
                                if (plot.isAuctioned()) {
                                    if (plot.getCurrentBidderId() != null) {
                                        IOfflinePlayer offlinePlayer = sob.getOfflinePlayer(plot.getCurrentBidderId());
                                        EconomyResponse economyResponse = sob.depositPlayer(offlinePlayer, plot.getCurrentBid());

                                        if (economyResponse.transactionSuccess()) {
                                            player.sendMessage(plot.getCurrentBidder() + " was refunded their money for their plot bid.");
                                        } else {
                                            player.sendMessage(economyResponse.errorMessage);
                                            warn(economyResponse.errorMessage);
                                        }
                                    }
                                }

                                if (pmi.isRefundClaimPriceOnReset() && plot.getOwnerId() != null) {
                                    IOfflinePlayer playerowner = sob.getOfflinePlayer(plot.getOwnerId());

                                    EconomyResponse er = sob.depositPlayer(playerowner, pmi.getClaimPrice());

                                    if (er.transactionSuccess()) {
                                        IPlayer playerOwner = sob.getPlayer(playerowner.getUniqueId());
                                        if (playerOwner.getName().equalsIgnoreCase(plot.getOwner())) {
                                            playerOwner.sendMessage(C("WordPlot") + " " + id + " " + C("MsgOwnedBy") + " " + plot.getOwner() + " " + C("MsgWasReset") + " " + Util().moneyFormat(pmi.getClaimPrice()));
                                        }
                                    } else {
                                        player.sendMessage("§c" + er.errorMessage);
                                        warn(er.errorMessage);
                                        return true;
                                    }
                                }
                            }

                            if (!PlotMeCoreManager.isPlotAvailable(id, pmi)) {
                                PlotMeCoreManager.removePlot(pmi, id);
                            }

                            PlotMeCoreManager.removeOwnerSign(world, id);
                            PlotMeCoreManager.removeSellSign(world, id);
                            plugin.getSqlManager().deletePlot(PlotMeCoreManager.getIdX(id), PlotMeCoreManager.getIdZ(id), world.getName().toLowerCase());

                            pmi.addFreed(id);

                            if (isAdvancedLogging()) {
                                sob.getLogger().info(LOG + player.getName() + " " + C("MsgResetPlot") + " " + id);
                            }
                        }
                    } else {
                        player.sendMessage("§c" + C("MsgThisPlot") + "(" + id + ") " + C("MsgNotYoursNotAllowedReset"));
                    }
                }
            } else {
                player.sendMessage("§c" + C("MsgNotPlotWorld"));
            }
        } else {
            player.sendMessage("§c" + C("MsgPermissionDenied"));
            return false;
        }
        return true;
    }
}
