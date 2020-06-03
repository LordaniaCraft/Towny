package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.WordUtils.capitalizeFully;

public class SiegeCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		boolean isSuccessful;
		try {
			if (!sender.hasPermission("towny.admin.siege")) throw new CMDError("You do not have permission to do this");
			if (args.length == 0) throw new CMDError("You have not provided enough arguments");
			if (!(sender instanceof Player)) throw new CMDError("You can't do that here");

			String prefix = ChatColor.GOLD + "[" +
				ChatColor.AQUA + "SiegeAdmin" +
				ChatColor.GOLD + "]";
			Player admin = (Player) sender;
			List<String> message = new ArrayList<String>();
			String subCommand = args[0].toLowerCase();

			switch (subCommand) {
				case "list": {
					message.add(prefix + ChatColor.GOLD + "-----------["
						+ ChatColor.AQUA
						+ "Active Sieges "
						+ ChatColor.GOLD + "]-----------");
					for (Siege siege : TownyUniverse.getInstance().getAllSieges()) {
						message.add(prefix +
							ChatColor.GOLD + "Siege Name: " +
							ChatColor.AQUA + siege.getName() +
							ChatColor.GOLD + " Current Points: " +
							ChatColor.AQUA + siege.getSiegePoints() +
							ChatColor.GOLD + " Status: " +
							ChatColor.AQUA + capitalizeFully(siege.getStatus().toString().replace("_", " ").toLowerCase())
						);
					}
					break;
				}

				case "pause": {
					Siege siege = isNearSiege(admin);
					if (siege == null) throw new CMDError("You are not near a siege");
					if (siege.getStatus() != SiegeStatus.IN_PROGRESS)
						throw new CMDError("That siege is not in progress");
					siege.setStatus(SiegeStatus.PAUSED);
					siege.setPausedTimeLeft();
					message.add(prefix + ChatColor.GOLD + "You have Paused " + ChatColor.AQUA + siege.getName());
					break;
				}
				case "resume": {
					Siege siege = isNearSiege(admin);
					if (siege == null) throw new CMDError("You are not near a siege");
					if (siege.getStatus() != SiegeStatus.PAUSED) throw new CMDError("That siege is not paused");
					siege.setStatus(SiegeStatus.IN_PROGRESS);
					siege.setScheduledEndTime((long) (System.currentTimeMillis() + siege.getPausedTimeLeft()));
					message.add(prefix + ChatColor.GOLD + "You have Resumed " + ChatColor.AQUA + siege.getName());
					break;
				}
				case "cancel": {
					// check if its not finished first
					Siege siege = isNearSiege(admin);
					if (siege == null) throw new CMDError("You are not near a siege");
					if (siege.getStatus() != SiegeStatus.IN_PROGRESS)
						throw new CMDError("That siege is not in progress");
					siege.setStatus(SiegeStatus.CANCELED);
					message.add(prefix + ChatColor.GOLD + "You have Canceled " + ChatColor.AQUA + siege.getName());
					SiegeWarMoneyUtil.giveWarChestToAttackingNation(siege);
					TownyUniverse.getInstance().getDataSource().removeSiege(siege);
					break;
				}

				case "setpoints": {
					//check if its not finished
					if (args.length < 2) throw new CMDError("You have not specified a number of points");
					int points = Integer.parseInt(args[1]);
					Siege siege = isNearSiege(admin);
					if (siege == null) throw new CMDError("You are not near a siege");
					siege.setSiegePoints(points);
					message.add(prefix +
						ChatColor.GOLD + "You have set Siege Points for " +
						ChatColor.AQUA + siege.getName() +
						ChatColor.GOLD + " to: " +
						ChatColor.AQUA + points
					);

					break;
				}
				case "tp": {
					if (args.length < 2) throw new CMDError("You have not specified siege to teleport to");
					Siege siege = doesSiegeExist(args[1]);
					if (siege == null) throw new CMDError("That siege doesn not exist");
					message.add(prefix +
						ChatColor.GOLD + "Teleporting to " +
						ChatColor.AQUA + siege.getName() +
						ChatColor.GOLD + " flag location");
					admin.teleport(siege.getFlagLocation());

					break;
				}
				default:
					throw new CMDError("Not a valid sub command");
			}
			String[] messageArray = new String[message.size()];
			message.toArray(messageArray);
			TownyMessaging.sendErrorMsg(sender, messageArray);
			isSuccessful = true;
		} catch (Throwable error) {
			// Use catch to send player input errors
			if (error instanceof CMDError) TownyMessaging.sendErrorMsg(sender, error.getMessage());
				// But still log normal errors
			else if (error instanceof NumberFormatException)
				TownyMessaging.sendErrorMsg(sender, "You have not provided a number");
			else error.printStackTrace();
			// Command usage will pop up with PlayerErrors
			// which i think is good UX
			isSuccessful = false;
		}
		return isSuccessful;
	}

	//does siege exist boolean returning for loop
	private static Siege doesSiegeExist(String siegeName) {
		Siege doesSiegeExist = null;
		for (Siege siege : TownyUniverse.getInstance().getAllSieges()) {
			if (!siegeName.equals(siege.getName().toLowerCase())) continue;
			doesSiegeExist = siege;
			break;
		}
		return doesSiegeExist;
	}

	//boolean for loop to find siege
	private static Siege isNearSiege(Player admin) {
		Siege doesSiegeExist = null;
		for (Siege siege : TownyUniverse.getInstance().getAllSieges()) {
			if (!SiegeWarPointsUtil.isPlayerInTimedPointZone(admin, siege)) continue;
			doesSiegeExist = siege;
			break;
		}
		return doesSiegeExist;
	}
}



class CMDError extends Exception {
	public CMDError(String message) {
		super(message);
	}

}