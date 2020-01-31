package me.arasple.mc.trhologram.hologram;

import com.google.common.collect.Lists;
import io.izzel.taboolib.util.Strings;
import me.arasple.mc.trhologram.nms.HoloPackets;
import me.arasple.mc.trhologram.utils.Locations;
import me.arasple.mc.trhologram.utils.Vars;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Arasple
 * @date 2020/1/29 20:07
 */
public class HologramContent {

    private Location location;
    private String text;
    private Object armorstand;
    private int id;

    public HologramContent(String text) {
        this.text = text;
    }

    public static List<HologramContent> createList(List<String> lines) {
        List<HologramContent> holoLines = Lists.newArrayList();
        lines.forEach(l -> holoLines.add(new HologramContent(l)));
        return holoLines;
    }

    public void updateLocation(Player... players) {
        HoloPackets.getInst().teleportArmorstand(getArmorstand(), location, players);
    }

    public void update(Player... players) {
        for (Player player : players) {
            HoloPackets.getInst().setArmorstandName(getArmorstand(), Vars.replace(player, text));
            HoloPackets.getInst().updateArmorstand(getArmorstand(), player);
        }
    }

    public void display(Player... players) {
        for (Player player : players) {
            HoloPackets.getInst().setArmorstandName(getArmorstand(), Vars.replace(player, text));
            HoloPackets.getInst().displayArmorstand(getArmorstand(), player);
        }
    }

    public void hide(Player... players) {
        for (Player player : players) {
            HoloPackets.getInst().setArmorstandVisible(getArmorstand(), false);
            HoloPackets.getInst().updateArmorstand(getArmorstand(), player);
            HoloPackets.getInst().setArmorstandVisible(getArmorstand(), true);
        }
    }

    public void destroy(Player... players) {
        HoloPackets.getInst().destroyArmorstand(getArmorstand(), players);
    }

    public void initArmorstand() {
        this.armorstand = HoloPackets.getInst().createArmorStand(location);
        setId(HoloPackets.getInst().getEntityId(getArmorstand()));
    }

    /*
    GETTERS & SETTERS
     */

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setLocation(Location location, List<Player> players) {
        this.location = location;

        if (!Locations.equals(this.location, location) && players.size() > 0) {
            players.forEach(this::updateLocation);
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Object getArmorstand() {
        if (!isArmorstandInited()) {
            initArmorstand();
        }
        return armorstand;
    }

    public void setArmorstand(Object armorstand) {
        this.armorstand = armorstand;
    }

    public boolean isArmorstandInited() {
        return armorstand != null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isEmpty() {
        return Strings.isEmpty(getText());
    }

}