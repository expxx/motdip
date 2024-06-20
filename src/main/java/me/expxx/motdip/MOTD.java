package me.expxx.motdip;

import com.velocitypowered.api.util.Favicon;

public class MOTD {
    public String motd;
    public Favicon fav;

    public  MOTD(String MOTD, Favicon fav) {
        this.fav = fav;
        this.motd = MOTD;
    }
}
