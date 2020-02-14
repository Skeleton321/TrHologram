package me.arasple.mc.trhologram.hologram;

import com.google.common.collect.Lists;
import io.izzel.taboolib.internal.apache.lang3.math.NumberUtils;
import me.arasple.mc.trhologram.TrHologram;
import me.arasple.mc.trhologram.action.base.AbstractAction;
import me.arasple.mc.trhologram.item.Mat;
import me.arasple.mc.trhologram.utils.JavaScript;
import me.arasple.mc.trhologram.utils.Locations;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

/**
 * @author Arasple
 * @date 2020/1/29 20:06
 */
public class Hologram {

    private String name;
    private List<UUID> viewers;
    private List<HologramContent> contents;
    private List<AbstractAction> actions;
    private Location location;
    private String viewCondition;
    private String viewDistance;
    private double finalViewDistance;
    private String loadedFrom;
    private int update;
    private List<Integer> updateIndexs;
    private BukkitRunnable task;

    public Hologram(String name, Location location, List<String> contents) {
        this(name, location, contents, Lists.newArrayList(), null, null, 40);
    }

    public Hologram(String name, Location location, List<String> contents, List<AbstractAction> actions) {
        this(name, location, contents, actions, null, null, 40);
    }

    public Hologram(String name, Location location, List<String> contents, List<AbstractAction> actions, String viewCondition, String viewDistance, int update) {
        this.name = name;
        this.location = location;
        this.contents = HologramContent.createList(contents);
        this.actions = actions;
        this.viewers = Lists.newArrayList();
        this.viewCondition = viewCondition;
        this.viewDistance = viewDistance;
        this.finalViewDistance = NumberUtils.toDouble(viewDistance, -1);
        this.update = update;
        this.updateIndexs = Lists.newArrayList();
        initOffsets();
        setUpdateIndexs();
    }

    public void runTask() {
        try {
            task.cancel();
        } catch (Throwable ignored) {
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        task.runTaskTimerAsynchronously(TrHologram.getPlugin(), update, update);
    }

    /**
     * 试图为这些玩家显示全息图
     *
     * @param players 玩家
     */
    public void display(Player... players) {
        for (Player player : players) {
            if (viewers.contains(player.getUniqueId())) {
                contents.forEach(l -> l.update(player));
            } else {
                contents.forEach(l -> l.display(player));
                viewers.add(player.getUniqueId());
            }
        }
    }

    /**
     * 为已展示的玩家更新全息图信息, 或者隐藏不再满足条件的全息图观察者
     */
    public void update() {
        getViewersAsPlayer().forEach(player -> {
            if (!isVisible(player)) {
                destroy(player);
            } else {
                getUpdateIndexs().forEach(i -> getContents().get(i).update(player));
            }
        });
        viewers.removeIf(uuid -> !Bukkit.getOfflinePlayer(uuid).isOnline() || !isVisible(Bukkit.getPlayer(uuid)));
    }

    /**
     * 摧毁玩家该显全息图
     *
     * @param players
     */
    public void destroy(Player... players) {
        contents.forEach(l -> l.destroy(players));
        for (Player player : players) {
            viewers.remove(player.getUniqueId());
        }
    }

    public void destroyAll() {
        getViewersAsPlayer().forEach(this::destroy);
    }

    /**
     * 玩家是否满足观察该全息图的条件
     *
     * @param player 玩家
     * @return 是否满足
     */
    public boolean isVisible(Player player) {
        if (player.getLocation().getWorld() != location.getWorld()) {
            return false;
        }
        double distance = finalViewDistance > 0 ? finalViewDistance : NumberUtils.toDouble(String.valueOf(JavaScript.run(player, viewDistance)), -1);
        return (distance <= 0 || player.getLocation().distance(location) <= distance) && (boolean) JavaScript.run(player, viewCondition);
    }

    private void initOffsets() {
        Location location = this.location.clone();
        getContents().forEach(content -> {
            content.setLocation(location.clone(), getViewersAsPlayer());
            location.subtract(0, 0.25, 0);
        });
        getContents().removeIf(HologramContent::isEmpty);
    }

    private void setUpdateIndexs() {
        updateIndexs.clear();
        for (int i = 0; i < getContents().size(); i++) {
            if (PlaceholderAPI.containsPlaceholders(getContents().get(i).getText())) {
                updateIndexs.add(i);
            }
        }
    }

    /*
    GETTERS & SETTERS
     */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UUID> getViewers() {
        return viewers;
    }

    public void setViewers(List<UUID> viewers) {
        this.viewers = viewers;
    }

    public List<Player> getViewersAsPlayer() {
        List<Player> players = Lists.newArrayList();
        viewers.forEach(viewer -> {
            Player player = Bukkit.getPlayer(viewer);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        });
        return players;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        if (!Locations.equals(this.location, location)) {
            this.location = location;
            initOffsets();
            getViewersAsPlayer().forEach(player -> contents.forEach(l -> l.updateLocation(player)));
        } else {
            this.location = location;
        }
    }

    public List<HologramContent> getContents() {
        return contents;
    }

    public void setContents(List<String> contents) {
        while (contents.size() < getContents().size()) {
            HologramContent hologramContent = getContents().get(getContents().size() - 1);
            getViewersAsPlayer().forEach(hologramContent::destroy);
            getContents().remove(getContents().size() - 1);
        }
        int size = getContents().size();
        for (int i = 0; i < contents.size(); i++) {
            if (size > i) {
                HologramContent content = getContents().get(i);
                if (content.getMat() != null || Mat.readMat(contents.get(i)) != null) {
                    getViewersAsPlayer().forEach(content::destroy);
                    getContents().set(i, new HologramContent(contents.get(i)));
                } else {
                    content.setText(contents.get(i));
                }
            } else {
                HologramContent content = new HologramContent(contents.get(i));
                getContents().add(content);
            }
        }
        initOffsets();
        setUpdateIndexs();
        getContents().forEach(content -> getViewersAsPlayer().forEach(content::display));
    }

    public List<AbstractAction> getActions() {
        return actions;
    }

    public void setActions(List<AbstractAction> actions) {
        this.actions = actions;
    }

    public String getViewCondition() {
        return viewCondition;
    }

    public void setViewCondition(String viewCondition) {
        this.viewCondition = viewCondition;
    }

    public String getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(String viewDistance) {
        this.viewDistance = viewDistance;
        this.finalViewDistance = NumberUtils.toDouble(viewDistance, -1);
    }

    public Object getExactViewDistance() {
        return NumberUtils.isParsable(getViewDistance()) ? NumberUtils.toDouble(getViewDistance(), -1) : getViewDistance();
    }

    public double getFinalViewDistance() {
        return finalViewDistance;
    }

    public void setFinalViewDistance(double finalViewDistance) {
        this.finalViewDistance = finalViewDistance;
    }

    public String getLoadedFrom() {
        return loadedFrom;
    }

    public void setLoadedFrom(String loadedFrom) {
        this.loadedFrom = loadedFrom;
    }

    public int getUpdate() {
        return update;
    }

    public void setUpdate(int update) {
        this.update = update;
    }


    public List<Integer> getUpdateIndexs() {
        return updateIndexs;
    }

    public boolean isViewing(Player player) {
        return getViewers().contains(player.getUniqueId());
    }

    public List<String> getContentsAsList() {
        List<String> contents = Lists.newArrayList();
        getContents().forEach(h -> contents.add(h.getText()));
        return contents;
    }

}
