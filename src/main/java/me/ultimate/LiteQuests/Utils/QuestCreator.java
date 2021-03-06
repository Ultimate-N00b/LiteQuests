package main.java.me.ultimate.LiteQuests.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import main.java.me.ultimate.LiteQuests.Language;
import main.java.me.ultimate.LiteQuests.LiteQuests;
import main.java.me.ultimate.LiteQuests.Enums.QuestType;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class QuestCreator implements Listener {

   static HashMap<String, Double> creators = new HashMap<String, Double>();
   static HashMap<String, String> name = new HashMap<String, String>();
   static HashMap<String, QuestType> type = new HashMap<String, QuestType>();
   static HashMap<String, Location> loc = new HashMap<String, Location>();
   static HashMap<String, EntityType> entity = new HashMap<String, EntityType>();
   static HashMap<String, Integer> killAmount = new HashMap<String, Integer>();

   public static void startCreator(final Player p, final String qName) {
      if (!creators.containsKey(p.getName())) {
         String questTypes = "| ";
         for (final QuestType v : QuestType.values()) {
            if (v != QuestType.None)
               questTypes = questTypes + v.name() + " | ";
         }
         Send.sendMessage(p, Language.STARTED_QUEST_CREATOR.replaceAll("%questtypes%", questTypes));
         creators.put(p.getName(), (double) 1);
         name.put(p.getName(), qName);
      } else {
         Send.sendMessage(p, Language.ALREADY_CREATING_QUEST);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onPlayerChat(final AsyncPlayerChatEvent event) {
      if (creators.containsKey(event.getPlayer().getName())) {
         event.setCancelled(true);
         final Player p = event.getPlayer();
         String msg = event.getMessage();
         final int words = msg.trim().isEmpty() ? 0 : msg.trim().split("\\s+").length;
         if (creators.get(p.getName()) == 1) {
            if (words == 1) {
               if (!msg.contains(".")) {
                  boolean invalid = true;
                  for (final QuestType t : QuestType.values()) {
                     if (msg.equals(t.name())) {
                        type.put(p.getName(), t);
                        creators.remove(p.getName());
                        creators.put(p.getName(), (double) 2);
                        invalid = false;
                        String n = null;
                        if (t.equals(QuestType.Location))
                           n = Language.LOCATION_SETUP;
                        if (t.equals(QuestType.MobKill))
                           n = Language.MOBKILL_SETUP;
                        Send.sendMessage(p,
                              Language.SET_QUEST_TYPE.replaceAll("%type%", t.name()).replaceAll("%next%", n));
                        return;
                     }
                  }
                  if (invalid) {
                     Send.sendMessage(p, Language.UNVALID_TYPE);
                  }
               } else {
                  Send.sendMessage(p, Language.NO_PERIODS);
               }
            } else {
               Send.sendMessage(p, Language.TOO_LONG);
            }
         }
         if (creators.get(p.getName()) > 1) {
            final QuestType qType = type.get(p.getName());
            if (creators.get(p.getName()) == 2) {
               if (qType.equals(QuestType.Location)) {
                  Send.sendMessage(p, Language.LOCATION_SET);
                  loc.put(p.getName(), p.getLocation());
                  finishCreator(p);
               } else if (qType.equals(QuestType.MobKill)) {
                  if (words == 1) {
                     boolean valid = false;
                     for (final EntityType ent : EntityType.values()) {
                        if (!valid && msg.equalsIgnoreCase(ent.name()) && ent.isAlive()) {
                           valid = true;
                           entity.put(p.getName(), ent);
                           creators.remove(p.getName());
                           creators.put(p.getName(), 2.5);
                           Send.sendMessage(p, Language.MOBKILL_SET.replaceAll("%entity%", ent.name().toLowerCase()));
                        }
                     }
                     if (!valid) {
                        Send.sendMessage(p, Language.UNVALID_TYPE);
                     }
                  } else {
                     Send.sendMessage(p, Language.TOO_LONG);
                  }
               }
            } else if (creators.get(p.getName()) == 2.5) {
               if (words == 1) {
                  if (!msg.contains(".") && IsInteger.check(msg)) {
                     Send.sendMessage(p, Language.SET_MOBKILL_AMOUNT.replaceAll("%int%", msg));
                     killAmount.put(p.getName(), Integer.parseInt(msg));
                     finishCreator(p);
                  } else {
                     Send.sendMessage(p, Language.INVALID_NUMBER);
                  }
               } else {
                  Send.sendMessage(p, Language.TOO_LONG);
               }
            }
         }
      } 
   }

   public void finishCreator(Player p) {
      //Setting up the files..
      File questsFile = new File(LiteQuests.dataFolder + File.separator + "Quests.yml");
      FileConfiguration questsConfig = YamlConfiguration.loadConfiguration(questsFile);
      try {
         questsConfig.save(questsFile);
      } catch (IOException e) {
         e.printStackTrace();
      }
      questsConfig = YamlConfiguration.loadConfiguration(questsFile);

      Send.sendMessage(p, Language.QUEST_CREATION_FINISHED);
      creators.remove(p.getName());

      String s = "Quests." + name.get(p.getName());
      name.remove(p.getName());

      QuestType qtype = type.get(p.getName());
      type.remove(p.getName());
      questsConfig.set(s + ".Type", qtype.name());

      if (qtype.equals(QuestType.Location)) {
         Location loca = loc.get(p.getName());
         loc.remove(p.getName());
         questsConfig.set(s + ".world", loca.getWorld().getName());
         questsConfig.set(s + ".x", loca.getBlockX());
         questsConfig.set(s + ".y", loca.getBlockY());
         questsConfig.set(s + ".z", loca.getBlockZ());
      } else if (qtype.equals(QuestType.MobKill)) {
         EntityType et = entity.get(p.getName());
         entity.remove(p.getName());

         int amount = killAmount.get(p.getName());
         killAmount.remove(p.getName());

         questsConfig.set(s + ".Entity", et.name());
         questsConfig.set(s + ".Amount", amount);

      }
      try {
         questsConfig.save(questsFile);
      } catch (IOException e) {
         e.printStackTrace();
      }

   }

   public void stopCreator(final Player p) {

   }

}
