package me.expxx.motdip;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.util.Favicon;
import dev.dejvokep.boostedyaml.YamlDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

public class ReloadCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        YamlDocument config = MotdIP.getConfig();
        try { config.reload(); } catch (IOException e) { e.printStackTrace(); }
        Logger logger = MotdIP.getLogger();
        Path dir = MotdIP.getDir();
        MotdIP.getLogger().warning("Reloading all MOTDs");

        MotdIP.getMap().clear();
        File deF = new File(dir + "/default_icon.png");
        if(!deF.exists()) {
            try {
                InputStream is = getClass().getResourceAsStream("default.png");
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
        MotdIP.setDefaultMotd(new MOTD(config.getString("Default.motd"), defaultIcon));

        Set<Object> set = config.getSection("Servers").getKeys();
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
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else { icon = finalDefaultIcon; }
            logger.info("Motd " + e + " loaded");
            MOTD m = new MOTD(motd, icon);
            MotdIP.getMap().put(ip, m);
        });

    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("motd.reload");
    }
}
