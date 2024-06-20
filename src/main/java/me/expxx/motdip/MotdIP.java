package me.expxx.motdip;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.Key;
import java.util.*;
import java.util.logging.Logger;

@Plugin(
        id = "motdip",
        name = "MotdIP",
        version = "1.0.0"
)
public class MotdIP {

    private static YamlDocument config;

    private static HashMap<String, MOTD> Map = new HashMap<>();
    private static MOTD defaultMotd;

    public static Path dir;
    public ProxyServer server;
    public static Logger logger;

    public static HashMap<String, MOTD> getMap() {
        return Map;
    }
    public static void setDefaultMotd(MOTD defaultMotd2) {
        defaultMotd = defaultMotd2;
    }

    public static YamlDocument getConfig() {
        return config;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static Path getDir() {
        return dir;
    }

    @Inject
    public MotdIP(ProxyServer serverResp, Logger loggerResp, @DataDirectory Path dirResp) {
        dir = dirResp;
        logger = loggerResp;
        server = serverResp;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        File configLocation = new File(dir + "/config.yml");
        if(!configLocation.exists()) {
            try {
                config = YamlDocument.create(configLocation, this.getClass().getClassLoader().getResourceAsStream("config.yml"),
                        GeneralSettings.DEFAULT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            try { config = YamlDocument.create(configLocation); } catch(IOException ex) { ex.printStackTrace(); }
        }
        File deF = new File(dir + "/default_icon.png");
        if(!deF.exists()) {
            try {
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("default.png");
                BufferedImage as = ImageIO.read(is);
                File outputfile = new File(dir + File.separator + "default_icon.png");
                ImageIO.write(as, "png", outputfile);
            }catch(Exception io) { io.printStackTrace(); }
        }
        Favicon defaultIcon = null;
        try {
            defaultIcon = Favicon.create(ImageIO.read(deF));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        defaultMotd = new MOTD(config.getString("Default.motd"), defaultIcon);

        config.set("Loaded", System.currentTimeMillis());
        try { config.save(); } catch(IOException ex) { ex.printStackTrace(); }
        Section sec = config.getSection("Servers");
        Set<Object> set = sec.getKeys();
        Favicon finalDefaultIcon = defaultIcon;
        set.forEach((e) -> {
            String motd = config.getString("Servers." + e + ".motd");
            String ip = config.getString("Servers." + e + ".ip");
            String file_name = config.getString("Servers." + e + ".file");
            Favicon icon = null;
            if(file_name != null) {
                try {
                    File f = new File(dir + File.separator + file_name);
                    icon = Favicon.create(ImageIO.read(f));
                    logger.info("Motd " + e + " loaded");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else { icon = finalDefaultIcon; }
            MOTD m = new MOTD(motd, icon);
            Map.put(ip, m);
        });

        CommandManager manager = server.getCommandManager();
        CommandMeta meta = manager.metaBuilder("mreload")
                .aliases()
                .plugin(this)
                .build();
        SimpleCommand reload = new ReloadCommand();
        manager.register(meta, reload);
    }

    @Subscribe
    public void motd(ProxyPingEvent e) {
        ServerPing ping = e.getPing();
        ServerPing.Version ver = e.getPing().getVersion();
        ServerPing.Players plr = e.getPing().getPlayers().orElse(null);
        String host = e.getConnection().getVirtualHost().get().getHostName();

        if(config.getBoolean("debug")) {
            getLogger().warning(host);
        }

        if(Map.containsKey(host)) {
            MOTD m = Map.get(host);
            Component desc = LegacyComponentSerializer.legacy('&').deserialize(m.motd);
            ping = new ServerPing(ver, plr, desc, m.fav);
        } else {
            Component desc = LegacyComponentSerializer.legacy('&').deserialize(defaultMotd.motd);
            ping = new ServerPing(ver, plr, desc, defaultMotd.fav);
        }
        e.setPing(ping);
        e.setResult(ResultedEvent.GenericResult.allowed());
    }
}
