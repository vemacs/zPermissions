/*
 * Copyright 2013 Allan Saddi <allan@saddi.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.Collection;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Core operations (usually concerning the online permissions system aka Bukkit)
 * called by command handlers and event handlers.
 * 
 * @author asaddi
 */
public interface ZPermissionsCore {

    // Refreshing the attachments of a set of players

    public void refreshPlayer(String playerName, RefreshCause cause);

    public void refreshPlayers();

    // NB called from async thread
    public void refreshPlayers(Collection<String> playerNames);

    public void refreshAffectedPlayers(String groupName);

    // Refreshing the temporary group membership timer

    public void refreshExpirations();

    public void refreshExpirations(String playerName);

    // Config + storage reload

    public void reload();

    // Storage reload

    public void refresh(boolean force, Runnable runnable);

    // Player attachment control

    public void setBukkitPermissions(Player player, Location location, boolean force, RefreshCause eventCause);

    public void removeBukkitPermissions(Player player, boolean recalculate);

    // Utility
    
    public Set<String> getRegions(Location location, Player player);

}
