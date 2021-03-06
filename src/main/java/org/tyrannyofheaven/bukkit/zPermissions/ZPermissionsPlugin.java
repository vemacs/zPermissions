/*
 * Copyright 2011 Allan Saddi <allan@saddi.com>
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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.error;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.broadcastAdmin;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.util.ToHDatabaseUtils;
import org.tyrannyofheaven.bukkit.util.ToHFileUtils;
import org.tyrannyofheaven.bukkit.util.ToHNamingConvention;
import org.tyrannyofheaven.bukkit.util.ToHSchemaVersion;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.VersionInfo;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.PermissionsResolver.ResolverResult;
import org.tyrannyofheaven.bukkit.zPermissions.command.DirTypeCompleter;
import org.tyrannyofheaven.bukkit.zPermissions.command.GroupTypeCompleter;
import org.tyrannyofheaven.bukkit.zPermissions.command.RootCommands;
import org.tyrannyofheaven.bukkit.zPermissions.command.TrackTypeCompleter;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.listener.ZPermissionsFallbackListener;
import org.tyrannyofheaven.bukkit.zPermissions.listener.ZPermissionsPlayerListener;
import org.tyrannyofheaven.bukkit.zPermissions.listener.ZPermissionsRegionPlayerListener;
import org.tyrannyofheaven.bukkit.zPermissions.model.DataVersion;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;
import org.tyrannyofheaven.bukkit.zPermissions.region.FactionsRegionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.region.RegionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.region.ResidenceRegionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.region.WorldGuardRegionStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.service.ZPermissionsServiceImpl;
import org.tyrannyofheaven.bukkit.zPermissions.storage.AvajeStorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.storage.MemoryStorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.ExpirationRefreshHandler;
import org.tyrannyofheaven.bukkit.zPermissions.util.ModelDumper;
import org.tyrannyofheaven.bukkit.zPermissions.util.RefreshTask;
import org.tyrannyofheaven.bukkit.zPermissions.vault.VaultChatBridge;
import org.tyrannyofheaven.bukkit.zPermissions.vault.VaultPermissionBridge;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebeaninternal.api.SpiEbeanServer;

/**
 * zPermissions main class.
 * 
 * @author asaddi
 */
public class ZPermissionsPlugin extends JavaPlugin implements ZPermissionsCore, ZPermissionsConfig {

    // Name of the default group, in absence of a config file
    private static final String DEFAULT_GROUP = "default";

    // Name of the default track, in absence of a config file
    private static final String DEFAULT_TRACK = "default";

    // Default dump directory
    private static final String DEFAULT_DUMP_DIRECTORY = "zPermissions-dumps";

    // Default timeout for temporary permissions
    private static final int DEFAULT_TEMP_PERMISSION_TIMEOUT = 60;

    // Default value for kick-on-error
    private static final boolean DEFAULT_KICK_ON_ERROR = true;

    // Default value for kick-ops-on-error
    private static final boolean DEFAULT_KICK_OPS_ON_ERROR = false;

    // Default value for region-support
    private static final boolean DEFAULT_REGION_SUPPORT_ENABLE = true;

    // Default value for assigned-groups-can-include-default
    private static final boolean DEFAULT_ASSIGNED_GROUPS_CAN_INCLUDE_DEFAULT = true;

    // Default max attempts (after the first) to complete a transaction
    private static final int DEFAULT_TXN_MAX_RETRIES = 1;

    // Default database support
    private static final boolean DEFAULT_DATABASE_SUPPORT = true;

    // Default number of ticks to wait between permissions refreshes of all players
    private static final int DEFAULT_BULK_REFRESH_DELAY = 5;

    // Default opaque inheritance
    private static final boolean DEFAULT_OPAQUE_INHERITANCE = true;

    // Default interleaved player permissions (to be changed at some future version)
    private static final boolean DEFAULT_INTERLEAVED_PLAYER_PERMISSIONS = true;

    // Default rank broadcast to admins
    private static final boolean DEFAULT_RANK_ADMIN_BROADCAST = false;

    // Default auto-refresh interval
    private static final int DEFAULT_AUTO_REFRESH_INTERVAL = -1;

    // Default auto-refresh forced refreshes
    private static final boolean DEFAULT_AUTO_REFRESH_FORCE = false;

    // Default primary group track
    private static final String DEFAULT_PRIMARY_GROUP_TRACK = null;

    // Default native Vault bridges
    private static final boolean DEFAULT_NATIVE_VAULT_BRIDGES = true;

    // Default Vault player prefix group fallback
    private static final boolean DEFAULT_VAULT_PREFIX_INCLUDES_GROUP = true;

    // Default Vault player metadata group fallback
    private static final boolean DEFAULT_VAULT_METADATA_INCLUDES_GROUP = true;

    // Default Vault behavior for playerInGroup()
    private static final boolean DEFAULT_VAULT_GROUP_TEST_USES_ASSIGNED_ONLY = false;

    // Default Vault behavior for getPlayerGroups()
    private static final boolean DEFAULT_VAULT_GET_GROUPS_USES_ASSIGNED_ONLY = false;

    // Filename of file-based storage
    private static final String FILE_STORAGE_FILENAME = "data.yml";

    // Name of metadata key for our PlayerState instances
    private static final String PLAYER_METADATA_KEY = "zPermissions.PlayerState";

    // Prefix for each player's dynamic permission
    public static final String DYNAMIC_PERMISSION_PREFIX = "zPermissions_player.";

    // Version info (may include build number)
    private VersionInfo versionInfo;

    // Permission resolver
    private PermissionsResolver resolver = new PermissionsResolver(this);

    // Model dumper
    private ModelDumper modelDumper;

    // Multi-user refreshing
    private final RefreshTask refreshTask = new RefreshTask(getZPermissionsCore(), this);

    // Our own Configuration (don't bother with JavaPlugin's)
    private FileConfiguration config;

    // The configured default track
    private String defaultTrack;

    // The configured dump directory
    private File dumpDirectory;

    // The configured default temporary permission timeout
    private int defaultTempPermissionTimeout;

    // Whether to kick users if there's any problem determining permissions
    private boolean kickOnError = DEFAULT_KICK_ON_ERROR;

    // If kickOnError is true, whether or not to kick operators too
    private boolean kickOpsOnError = DEFAULT_KICK_OPS_ON_ERROR;

    // If WorldGuard is present and this is true, enable region support
    private boolean regionSupportEnable;

    // Track definitions
    private final Map<String, List<String>> tracks = new LinkedHashMap<String, List<String>>();

    // Names of tracks (in original case)
    private final Set<String> trackNames = new LinkedHashSet<String>();

    // Whether or not to use the database (Avaje) storage strategy
    private boolean databaseSupport;

    // Maximum number of times to retry transactions (so total attempts is +1)
    private int txnMaxRetries;

    // Interval for auto-refresh
    private int autoRefreshInterval;

    // Whether or not auto-refreshes should be forced refreshes
    private boolean autoRefreshForce;

    // Task ID for auto-refresh task
    private int autoRefreshTaskId = -1;

    // Default primary group track
    private String defaultPrimaryGroupTrack;

    // Whether to use native Vault bridges
    private boolean nativeVaultBridges;

    // Whether the native Vault bridge's getPlayerPrefix/getPlayerSuffix should
    // fall back to the primary group
    private boolean vaultPrefixIncludesGroup;

    // Wehther the native Vault bridge's getPlayerInfo* methods should
    // fall back to the primary group
    private boolean vaultMetadataIncludesGroup;

    // Whether Vault playerInGroup() should use assigned groups only
    private boolean vaultGroupTestUsesAssignedOnly;
    
    // Whether Vault getPlayerGroups() should use assigned groups only
    private boolean vaultGetGroupsUsesAssignedOnly;

    // Strategy for permissions storage
    private StorageStrategy storageStrategy;

    // Create our own instance rather than use Bukkit's
    private EbeanServer ebeanServer;

    // Custom NamingConvention for Avaje
    private final ToHNamingConvention namingConvention = new ToHNamingConvention(this, "zperms_schema_version");

    // Backwards compatibility. Broadcast to admins if true, the custom permissions otherwise.
    private boolean rankAdminBroadcast = DEFAULT_RANK_ADMIN_BROADCAST;

    // Handles async refreshes when memberships expire
    private ExpirationRefreshHandler expirationRefreshHandler;

    // Strategy for region detection
    private RegionStrategy regionStrategy;

    // Region managers to use in preference order
    private List<String> regionManagers;

    /**
     * Retrieve this plugin's retrying TransactionStrategy
     * FIXME We use a separate TransactionStrategy because not all transactions
     * might be safe to be retried. Most simple transactions are safe to retry.
     * The ones that perform calculations or other operations (most notably
     * the rank commands) will have to be dealt with another way...
     * 
     * @return the retrying TransactionStrategy
     */
    private TransactionStrategy getRetryingTransactionStrategy() {
        return storageStrategy.getRetryingTransactionStrategy();
    }

    /**
     * Retrieve this plugin's DAO.
     * 
     * @return the DAO
     */
    PermissionDao getDao() {
        return storageStrategy.getDao();
    }

    // Retrieve the PermissionResolver instance
    private PermissionsResolver getResolver() {
        return resolver;
    }

    // Retrieve ModelDumper instance
    private ModelDumper getModelDumper() {
        return modelDumper;
    }

    // Retrieve ZPermissionsCore instance
    private ZPermissionsCore getZPermissionsCore() {
        return this;
    }
    
    // Retrieve ZPermissionsConfig instance
    private ZPermissionsConfig getZPermissionsConfig() {
        return this;
    }

    @Override
    public void onLoad() {
        versionInfo = ToHUtils.getVersion(this);
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onDisable()
     */
    @Override
    public void onDisable() {
        // Shut down region manager
        if (regionStrategy != null)
            regionStrategy.shutdown();

        // Kill expiration handler
        if (expirationRefreshHandler != null)
            expirationRefreshHandler.shutdown();

        // Kill pending refresh, if any
        refreshTask.stop();

        // Really shut off all async tasks
        getServer().getScheduler().cancelTasks(this);

        // Ensure storage is shut down properly
        if (storageStrategy != null)
            storageStrategy.shutdown();

        // Clear any player state

        // Remove permissions
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeBukkitPermissions(player, true);
        }

        log(this, "%s disabled.", versionInfo.getVersionString());
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onEnable()
     */
    @Override
    public void onEnable() {
        try {
            log(this, "%s starting...", versionInfo.getVersionString());

            // FIXME Defaults workaround, to be removed after 1.0
            boolean isUpgrade = new File(getDataFolder(), "config.yml").exists();
            // FIXME Defaults workaround end

            // Read config
            config = ToHFileUtils.getConfig(this);
            config.options().header(null);
            // FIXME Defaults workaround, to be removed after 1.0
            if (!isUpgrade)
                config.set("interleaved-player-permissions", false);
            // FIXME Defaults workaround end
            readConfig();

            // Upgrade/create config
            ToHFileUtils.upgradeConfig(this, config);

            initializeStorageStrategy();

            modelDumper = new ModelDumper(storageStrategy, this);

            // Install our commands
            (new ToHCommandExecutor<ZPermissionsPlugin>(this, new RootCommands(getZPermissionsCore(), storageStrategy, getResolver(), getModelDumper(), getZPermissionsConfig(), this)))
                .registerTypeCompleter("group", new GroupTypeCompleter(getDao()))
                .registerTypeCompleter("track", new TrackTypeCompleter(getZPermissionsConfig()))
                .registerTypeCompleter("dump-dir", new DirTypeCompleter(getZPermissionsConfig()))
                .setQuoteAware(true)
                .registerCommands();

            // Detect a region manager
            initializeRegionStrategy();
            boolean regionSupport = regionStrategy != null && regionSupportEnable; // Need both

            // Install our listeners
            expirationRefreshHandler = new ExpirationRefreshHandler(getZPermissionsCore(), storageStrategy, this);
            Bukkit.getPluginManager().registerEvents(new ZPermissionsPlayerListener(getZPermissionsCore(), this), this);
            if (regionSupport) {
                Bukkit.getPluginManager().registerEvents(new ZPermissionsRegionPlayerListener(getZPermissionsCore()), this);
                log(this, "%s region support: %s", regionStrategy.getName(), regionStrategy.isEnabled() ? "Enabled" : "Waiting");
            }

            // Set up service API
            ZPermissionsService service = new ZPermissionsServiceImpl(this, getResolver(), getDao(), getRetryingTransactionStrategy(), getZPermissionsConfig());
            getServer().getServicesManager().register(ZPermissionsService.class, service, this, ServicePriority.Normal);

            if (nativeVaultBridges && Bukkit.getPluginManager().getPlugin("Vault") != null) {
                // Set up Vault bridges
                new VaultPermissionBridge(this, storageStrategy, getZPermissionsCore(), service, getZPermissionsConfig()).register();
                log(this, "Installed native Vault Permissions bridge");
                new VaultChatBridge(this, storageStrategy, service, getZPermissionsConfig()).register();
                log(this, "Installed native Vault Chat bridge");
            }

            // Make sure everyone currently online has permissions
            // NB Do in foreground
            for (Player player : Bukkit.getOnlinePlayers()) {
                refreshPlayer(player.getName(), RefreshCause.GROUP_CHANGE);
            }

            // Start auto-refresh task, if one is configured
            startAutoRefreshTask();

            // Initialize expiration handler
            refreshExpirations();

            log(this, "%s enabled.", versionInfo.getVersionString());
        }
        catch (Throwable t) {
            error(this, "Failed to initialize:", t);
            if (kickOnError) {
                error(this, "ALL %sLOG-INS DISALLOWED", kickOpsOnError ? "" : "NON-OP ");
                Bukkit.getPluginManager().registerEvents(new ZPermissionsFallbackListener(kickOpsOnError), this);
            }

            // Not really supposed to eat Errors...
            if (t instanceof Error)
                throw (Error)t;
        }
    }

    private void initializeStorageStrategy() {
        // Set up TransactionStrategy and DAO
        storageStrategy = null;
        if (databaseSupport) {
            ebeanServer = ToHDatabaseUtils.createEbeanServer(this, getClassLoader(), namingConvention, config);

            SpiEbeanServer spiEbeanServer = (SpiEbeanServer)ebeanServer;
            if (spiEbeanServer.getDatabasePlatform().getName().contains("sqlite")) {
                log(this, Level.WARNING, "This plugin is NOT compatible with SQLite.");
                log(this, Level.WARNING, "Edit bukkit.yml to switch databases or disable database support in config.yml.");
                log(this, Level.WARNING, "Falling back to file-based storage strategy.");
                // Do nothing else (storageStrategy still null)
            }
            else {
                try {
                    ToHDatabaseUtils.upgradeDatabase(this, namingConvention, getClassLoader(), "sql");
                }
                catch (IOException e) {
                    error(this, "Exception upgrading database schema:", e);
                }

                log(this, "Using database storage strategy.");
                storageStrategy = new AvajeStorageStrategy(this, txnMaxRetries);
            }
        }
        
        // If still no storage strategy at this point, use flat-file one
        if (storageStrategy == null) {
            log(this, "Using file-based storage strategy.");
            storageStrategy = new MemoryStorageStrategy(this, new File(getDataFolder(), FILE_STORAGE_FILENAME));
        }
        
        // Initialize storage strategy
        try {
            storageStrategy.init();
        }
        catch (Exception e) {
            error(this, "Exception initializing storage strategy:", e);
            // TODO Now what?
        }
    }

    private void initializeRegionStrategy() {
        regionStrategy = null;

        if (!regionSupportEnable)
            return; // Don't bother with the rest

        Map<String, RegionStrategy> strategies = new LinkedHashMap<String, RegionStrategy>();
        RegionStrategy regionStrategy;

        // WorldGuard
        regionStrategy = new WorldGuardRegionStrategy(this, getZPermissionsCore());
        strategies.put(regionStrategy.getName(), regionStrategy);

        // Additional region managers are registered here.
        regionStrategy = new ResidenceRegionStrategy(this, getZPermissionsCore());
        strategies.put(regionStrategy.getName(), regionStrategy);
        
        regionStrategy = new FactionsRegionStrategy(this, getZPermissionsCore());
        strategies.put(regionStrategy.getName(), regionStrategy);

        // Run through list in preference order
        for (String rmName : regionManagers) {
            regionStrategy = strategies.get(rmName);
            if (regionStrategy == null) {
                // Misconfiguration
                warn(this, "Unknown region manager '%s'. Valid values are: %s", rmName, ToHStringUtils.delimitedString(", ", strategies.keySet()));
                continue;
            }
            
            if (regionStrategy.isPresent()) {
                debug(this, "Found region manager %s", regionStrategy.getName());
                regionStrategy.init();
                this.regionStrategy = regionStrategy;
                return;
            }
        }
    }

    @Override
    public EbeanServer getDatabase() {
        return ebeanServer;
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#getDatabaseClasses()
     */
    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> result = new ArrayList<Class<?>>();
        result.add(ToHSchemaVersion.class);
        result.add(PermissionEntity.class);
        result.add(Inheritance.class);
        result.add(PermissionRegion.class);
        result.add(PermissionWorld.class);
        result.add(Entry.class);
        result.add(Membership.class);
        result.add(EntityMetadata.class);
        result.add(DataVersion.class);
        return result;
    }

    // Remove all state associated with a player
    @Override
    public void removeBukkitPermissions(Player player, boolean recalculate) {
        debug(this, "Removing permissions for %s", player.getName());
        // NB Attachment is recycled along with the player instance

        // Disassociate PlayerState
        player.removeMetadata(PLAYER_METADATA_KEY, this);

        // Remove dynamic permission and recalculate, if wanted
        final String permName = DYNAMIC_PERMISSION_PREFIX + player.getName();
        Bukkit.getPluginManager().removePermission(permName);
        if (recalculate) {
            for (Permissible p : Bukkit.getPluginManager().getPermissionSubscriptions(permName)) {
                p.recalculatePermissions();
            }
        }
    }
    
    // Update state about a player, resolving effective permissions and
    // creating/updating their attachment
    @Override
    public void setBukkitPermissions(Player player, Location location, boolean force, RefreshCause eventCause) {
        boolean changed = false;
        try {
            changed = setBukkitPermissionsInternal(player, location, force);
        }
        catch (Error e) {
            throw e; // Never catch errors
        }
        catch (Throwable t) {
            error(this, "Exception while updating permissions for %s", player.getName(), t);
            broadcastAdmin(this, colorize("{RED}SEVERE error while determining permissions; see server.log!"));
            
            // Kick the player, if configured to do so
            if (kickOnError && (kickOpsOnError || !player.isOp())) {
                // Probably safer to do this synchronously
                final String playerName = player.getName();
                getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                    @Override
                    public void run() {
                        Player player = getServer().getPlayerExact(playerName);
                        if (player != null)
                            player.kickPlayer("Error determining your permissions");
                    }
                });
            }
            else {
                // Ensure player has no permissions
                removeBukkitPermissions(player, true);
                sendMessage(player, colorize("{RED}Error determining your permissions; all permissions removed!"));
            }
        }
        
        // Fire off event if requested and changed
        if (eventCause != null && changed) {
            final String playerName = player.getName();
            // Translate RefreshEvent to ZPermissionsPlayerPermissionsChangeEvent.Cause
            // Kinda dumb, but I don't want internal code to depend on the event class.
            final ZPermissionsPlayerUpdateEvent.Cause cause;
            switch (eventCause) {
            case COMMAND:
                cause = ZPermissionsPlayerUpdateEvent.Cause.COMMAND;
                break;
            case GROUP_CHANGE:
                cause = ZPermissionsPlayerUpdateEvent.Cause.GROUP_CHANGE;
                break;
            case MOVEMENT:
                cause = ZPermissionsPlayerUpdateEvent.Cause.MOVEMENT;
                break;
            default:
                throw new AssertionError("Unhandled RefreshCause: " + eventCause);
            }
            // Fire it off on the following tick
            Bukkit.getScheduler().runTask(this, new Runnable() {
                @Override
                public void run() {
                    Player player = Bukkit.getPlayerExact(playerName);
                    if (player != null) {
                        ZPermissionsPlayerUpdateEvent event = new ZPermissionsPlayerUpdateEvent(player, cause);
                        Bukkit.getPluginManager().callEvent(event);
                    }
                }
            });
        }
    }

    // Simulate failures probabilistically
//    private final java.util.Random failureChance = new java.util.Random();
//    private void fakeFailureChance() {
//        if (failureChance.nextDouble() < 0.2)
//            throw new RuntimeException("Oh noes! An error!");
//    }

    // Update state about a player, resolving effective permissions and
    // creating/updating their attachment
    private boolean setBukkitPermissionsInternal(final Player player, Location location, boolean force) {
        final Set<String> regions = getRegions(location, player);

        // Fetch existing state
        final String permName = DYNAMIC_PERMISSION_PREFIX + player.getName();
        Permission perm = Bukkit.getPluginManager().getPermission(permName);

        PlayerState playerState = getPlayerState(player);

        boolean hasPermissionAttachment = player.hasPermission(permName);

        // Check if the player is missing any state or changed worlds/regions
        if (!force) {
            force = perm == null || 
                    playerState == null ||
                    !hasPermissionAttachment ||
                    !regions.equals(playerState.getRegions()) ||
                    !location.getWorld().getName().equals(playerState.getWorld());
        }

        // No need to update yet (most likely called by movement-based event)
        if (!force) return false;

        debug(this, "Updating permissions for %s", player.getName());
        debug(this, "  location = %s", location);
        debug(this, "  regions = %s", regions);

        // Resolve effective permissions
        final String world = location.getWorld().getName().toLowerCase();
        ResolverResult resolverResult = getRetryingTransactionStrategy().execute(new TransactionCallback<ResolverResult>() {
            @Override
            public ResolverResult doInTransaction() throws Exception {
//                fakeFailureChance();
                return getResolver().resolvePlayer(player.getName(), world, regions);
            }
        }, true);

        debug(this, "(Existing Permission: %s, PlayerState: %s, PermissionAttachment: %s)", perm != null, playerState != null, hasPermissionAttachment);

        // Create dynamic permission to hold all permissions this player should have at this moment
        if (perm == null) {
            // NB This implicitly calls recalculatePermissibles(). However, since it has not been
            // added yet, permissibles will not pick up its children.
            perm = new Permission(permName, PermissionDefault.FALSE, resolverResult.getPermissions());
            Bukkit.getPluginManager().addPermission(perm);
        }
        else {
            perm.getChildren().clear();
            perm.getChildren().putAll(resolverResult.getPermissions());
        }
        // If player already has an attachment, then it will recalculate here.
        // Otherwise subscribers will be empty and nothing really happens. The
        // recalculation will then occur when the attachment is added below.
        perm.recalculatePermissibles();

        if (playerState != null) {
            // Update values
            playerState.setRegions(regions);
            playerState.setWorld(location.getWorld().getName());
            playerState.setGroups(resolverResult.getGroups());
        }
        else {
            // Create brand new PlayerState
            playerState = new PlayerState(regions, location.getWorld().getName(), resolverResult.getGroups());
            player.setMetadata(PLAYER_METADATA_KEY, new FixedMetadataValue(this, playerState));
        }
        
        // Finally, create attachment if missing
        if (!hasPermissionAttachment) {
            player.addAttachment(this, perm.getName(), true);
        }

        return true;
    }

    /**
     * Returns names of regions that contain the location
     * 
     * @param location the location
     * @param player the player in question (for supplemental info)
     * @return set of region names containing location
     */
    public Set<String> getRegions(Location location, Player player) {
        if (regionSupportEnable && regionStrategy != null && regionStrategy.isEnabled()) {
            return regionStrategy.getRegions(location, player);
        }
        return Collections.emptySet();
    }

    /**
     * Refresh a particular player's attachment (and therefore, effective
     * permissions). Only does something if the player is actually online.
     * 
     * @param playerName the name of the player
     */
    @Override
    public void refreshPlayer(String playerName, RefreshCause cause) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            debug(this, "Refreshing player %s", player.getName());
            setBukkitPermissions(player, player.getLocation(), true, cause);
        }
    }

    /**
     * Refresh the attachments of all online players.
     */
    @Override
    public void refreshPlayers() {
        debug(this, "Refreshing all online players");
        Set<String> toRefresh = new HashSet<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            toRefresh.add(player.getName());
        }
        refreshTask.start(toRefresh);
    }

    /**
     * Refresh the attachments of a specific set of players.
     * 
     * @param playerNames collection of players to refresh
     */
    @Override
    public void refreshPlayers(Collection<String> playerNames) {
        refreshTask.start(playerNames);
    }

    /**
     * Refresh expiration task.
     */
    @Override
    public void refreshExpirations() {
        expirationRefreshHandler.rescan();
    }

    /**
     * Refresh expiration task if the given player is online.
     * 
     * @param playerName a player
     */
    @Override
    public void refreshExpirations(String playerName) {
        if (Bukkit.getPlayerExact(playerName) != null)
            refreshExpirations();
    }

    /**
     * Refresh all players who are members of the given group.
     * 
     * @param groupName the affected group
     */
    @Override
    public void refreshAffectedPlayers(String groupName) {
        groupName = groupName.toLowerCase();
        Set<String> toRefresh = new HashSet<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerState playerState = getPlayerState(player);
            if (playerState == null || playerState.getGroups().contains(groupName)) {
                toRefresh.add(player.getName());
            }
        }
        
        if (toRefresh.isEmpty())
            return; // Nothing to do

        if (getLogger().isLoggable(Level.FINE))
            debug(this, "Refreshing players: %s", ToHStringUtils.delimitedString(", ", toRefresh));
        refreshTask.start(toRefresh);
    }

    /**
     * Retrieve the configured default track.
     * 
     * @return the default track
     */
    @Override
    public String getDefaultTrack() {
        return defaultTrack;
    }

    /**
     * Retrieve the list of groups for the given track.
     * 
     * @param trackName the name of the track
     * @return a list of groups (in ascending order) associated with the track
     */
    @Override
    public List<String> getTrack(String trackName) {
        return tracks.get(trackName.toLowerCase());
    }

    /**
     * Retrieve names of all tracks.
     * 
     * @return names of all tracks
     */
    @Override
    public List<String> getTracks() {
        return new ArrayList<String>(trackNames);
    }

    /**
     * Returns the configured dump directory.
     * 
     * @return the dump directory
     */
    @Override
    public File getDumpDirectory() {
        return dumpDirectory;
    }

    /**
     * Returns the configured default temporary permission timeout.
     * 
     * @return the temp permission timeout in seconds
     */
    @Override
    public int getDefaultTempPermissionTimeout() {
        return defaultTempPermissionTimeout;
    }

    /**
     * Returns the configured value for rank-admin-broadcast.
     * 
     * @return whether to broadcast rank changes to admins
     */
    @Override
    public boolean isRankAdminBroadcast() {
        return rankAdminBroadcast;
    }

    /**
     * Returns the default track used to determine the primary group.
     * 
     * @return the default primary group track
     */
    @Override
    public String getDefaultPrimaryGroupTrack() {
        return defaultPrimaryGroupTrack;
    }

    @Override
    public boolean isVaultPrefixIncludesGroup() {
        return vaultPrefixIncludesGroup;
    }

    @Override
    public boolean isVaultMetadataIncludesGroup() {
        return vaultMetadataIncludesGroup;
    }

    @Override
    public boolean isVaultGroupTestUsesAssignedOnly() {
        return vaultGroupTestUsesAssignedOnly;
    }

    @Override
    public boolean isVaultGetGroupsUsesAssignedOnly() {
        return vaultGetGroupsUsesAssignedOnly;
    }

    // Read config.yml
    private void readConfig() {
        // Set debug logging
        getLogger().setLevel(config.getBoolean("debug", false) ? Level.FINE : null);

        // Barebones defaults
        databaseSupport = config.getBoolean("database-support", DEFAULT_DATABASE_SUPPORT);
        getResolver().setDefaultGroup(DEFAULT_GROUP);
        defaultTrack = DEFAULT_TRACK;
        dumpDirectory = new File(DEFAULT_DUMP_DIRECTORY);
        getResolver().setGroupPermissionFormats(null);
        getResolver().setAssignedGroupPermissionFormats(null);
        tracks.clear();
        trackNames.clear();
        defaultPrimaryGroupTrack = DEFAULT_PRIMARY_GROUP_TRACK;
        
        String value;
        
        // Read values, set accordingly
        Object strOrList = config.get("group-permission");
        if (strOrList != null) {
            if (strOrList instanceof String) {
                if (hasText((String)strOrList))
                    getResolver().setGroupPermissionFormats(Collections.singleton((String)strOrList));
            }
            else if (strOrList instanceof List<?>) {
                Set<String> groupPerms = new HashSet<String>();
                for (Object obj : (List<?>)strOrList) {
                    if (obj instanceof String) {
                        groupPerms.add((String)obj);
                    }
                    else
                        warn(this, "group-permission list contains non-string value");
                }
                getResolver().setGroupPermissionFormats(groupPerms);
            }
            else
                warn(this, "group-permission must be a string or list of strings");
        }

        // TODO refactor
        strOrList = config.get("assigned-group-permission");
        if (strOrList != null) {
            if (strOrList instanceof String) {
                if (hasText((String)strOrList))
                    getResolver().setAssignedGroupPermissionFormats(Collections.singleton((String)strOrList));
            }
            else if (strOrList instanceof List<?>) {
                Set<String> groupPerms = new HashSet<String>();
                for (Object obj : (List<?>)strOrList) {
                    if (obj instanceof String) {
                        groupPerms.add((String)obj);
                    }
                    else
                        warn(this, "assigned-group-permission list contains non-string value");
                }
                getResolver().setAssignedGroupPermissionFormats(groupPerms);
            }
            else
                warn(this, "assigned-group-permission must be a string or list of strings");
        }

        getResolver().setOpaqueInheritance(config.getBoolean("opaque-inheritance", DEFAULT_OPAQUE_INHERITANCE));
        getResolver().setInterleavedPlayerPermissions(config.getBoolean("interleaved-player-permissions", DEFAULT_INTERLEAVED_PLAYER_PERMISSIONS));
        getResolver().setIncludeDefaultInAssigned(config.getBoolean("assigned-groups-can-include-default", DEFAULT_ASSIGNED_GROUPS_CAN_INCLUDE_DEFAULT));

        value = config.getString("default-group");
        if (hasText(value))
            getResolver().setDefaultGroup(value);
        
        value = config.getString("default-track");
        if (hasText(value))
            defaultTrack = value;

        value = config.getString("dump-directory");
        if (hasText(value))
            dumpDirectory = new File(value);

        value = config.getString("default-primary-group-track");
        if (hasText(value))
            defaultPrimaryGroupTrack = value;

        defaultTempPermissionTimeout = config.getInt("default-temp-permission-timeout", DEFAULT_TEMP_PERMISSION_TIMEOUT);
        txnMaxRetries = config.getInt("txn-max-retries", DEFAULT_TXN_MAX_RETRIES); // FIXME hidden
        rankAdminBroadcast = config.getBoolean("rank-admin-broadcast", DEFAULT_RANK_ADMIN_BROADCAST);

        // Read tracks, if any
        ConfigurationSection node = config.getConfigurationSection("tracks");
        if (node != null) {
            for (String trackName : node.getKeys(false)) {
                List<?> list = node.getList(trackName);
                if (list == null) {
                    warn(this, "Track %s must have a list value", trackName);
                    continue;
                }

                List<String> members = new ArrayList<String>();
                for (Object o : list) {
                    members.add(o.toString());
                }
                tracks.put(trackName.toLowerCase(), members);
                trackNames.add(trackName);
            }
        }
        // Set up default track if none are defined
        if (tracks.isEmpty()) {
            List<String> members = new ArrayList<String>();
            members.add("default");
            members.add("somegroup");
            members.add("someothergroup");
            tracks.put("default", members);
            trackNames.add("default");
        }

        kickOnError = config.getBoolean("kick-on-error", DEFAULT_KICK_ON_ERROR);
        kickOpsOnError = config.getBoolean("kick-ops-on-error", DEFAULT_KICK_OPS_ON_ERROR);
        regionSupportEnable = config.getBoolean("region-support", DEFAULT_REGION_SUPPORT_ENABLE);

        // FIXME currently hidden option
        refreshTask.setDelay(config.getInt("bulk-refresh-delay", DEFAULT_BULK_REFRESH_DELAY));
        autoRefreshInterval = config.getInt("auto-refresh-interval", DEFAULT_AUTO_REFRESH_INTERVAL);
        autoRefreshForce = config.getBoolean("auto-refresh-force", DEFAULT_AUTO_REFRESH_FORCE);
        nativeVaultBridges = config.getBoolean("native-vault-bridges", DEFAULT_NATIVE_VAULT_BRIDGES);
        vaultPrefixIncludesGroup = config.getBoolean("vault-prefix-includes-group", DEFAULT_VAULT_PREFIX_INCLUDES_GROUP);
        vaultMetadataIncludesGroup = config.getBoolean("vault-metadata-includes-group", DEFAULT_VAULT_METADATA_INCLUDES_GROUP);
        vaultGroupTestUsesAssignedOnly = config.getBoolean("vault-group-test-uses-assigned-only", DEFAULT_VAULT_GROUP_TEST_USES_ASSIGNED_ONLY);
        vaultGetGroupsUsesAssignedOnly = config.getBoolean("vault-get-groups-uses-assigned-only", DEFAULT_VAULT_GET_GROUPS_USES_ASSIGNED_ONLY);

        ToHDatabaseUtils.populateNamingConvention(config, namingConvention);

        // Region managers
        regionManagers = new ArrayList<String>();
        strOrList = config.get("region-managers");
        if (strOrList != null) {
            if (strOrList instanceof String) {
                regionManagers.add((String)strOrList);
            }
            else if (strOrList instanceof List<?>) {
                for (Object obj : (List<?>)strOrList) {
                    if (obj instanceof String) {
                        regionManagers.add((String)obj);
                    }
                    else
                        warn(this, "region-managers list contains non-string value");
                }
            }
            else
                warn(this, "region-managers must be a string or list of strings");
        }
        else {
            // Set up default region manager(s)
            regionManagers.add("WorldGuard");
            regionManagers.add("Residence");
        }

        configureWorldMirrors();
    }

    private void configureWorldMirrors() {
        getResolver().clearWorldAliases();
        ConfigurationSection mirrors = config.getConfigurationSection("mirrors");
        boolean header = false;
        if (mirrors != null) {
            for (String target : mirrors.getKeys(false)) {
                List<?> list = mirrors.getList(target);
                if (list == null) {
                    warn(this, "Mirror %s must be a list", target);
                    continue;
                }
                
                for (Object o : list) {
                    getResolver().addWorldAlias(o.toString(), target);
                    
                    if (!header) {
                        debug(this, "World mirrors:");
                        header = true;
                    }
                    
                    debug(this, "  %s -> %s", o.toString(), target);
                }
            }
        }
    }

    /**
     * Re-read config.yml and refresh attachments of all online players.
     */
    @Override
    public void reload() {
        config = ToHFileUtils.getConfig(this);
        readConfig();
        startAutoRefreshTask();
        refresh(true, new Runnable() {
            @Override
            public void run() {
                refreshPlayers();
                refreshExpirations();
            }
        });
    }

    /**
     * Refresh permissions store
     */
    @Override
    public void refresh(boolean force, Runnable finishTask) {
        storageStrategy.refresh(force, finishTask);
    }

    // Cancel existing auto-refresh task and start a new one if autoRefreshInterval is valid
    private void startAutoRefreshTask() {
        // Cancel previous task, if any
        if (autoRefreshTaskId > -1) {
            Bukkit.getScheduler().cancelTask(autoRefreshTaskId);
            autoRefreshTaskId = -1;
        }
        // Start up new task at new interval
        if (autoRefreshInterval > 0) {
            final Plugin plugin = this;
            autoRefreshTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    log(plugin, "Refreshing from database...");
                    refresh(autoRefreshForce, new Runnable() {
                        @Override
                        public void run() {
                            // This is executed after the storage refresh is done.
                            log(plugin, "Refresh done.");
                            refreshPlayers();
                            refreshExpirations();
                        }
                    });
                }
            }, autoRefreshInterval * 20 * 60, autoRefreshInterval * 20 * 60); // FIXME magic numbers
        }
    }

    // Retrieve associated PlayerState, if any
    private PlayerState getPlayerState(Player player) {
        for (MetadataValue mv : player.getMetadata(PLAYER_METADATA_KEY)) {
            if (mv.getOwningPlugin() == this) {
                return (PlayerState)mv.value();
            }
        }
        return null;
    }

    // Encapsulates state about a player
    private static class PlayerState {
        
        private Set<String> regions;

        private String world;

        private Set<String> groups;

        public PlayerState(Set<String> regions, String world, Set<String> groups) {
            setRegions(regions);
            setWorld(world);
            setGroups(groups);
        }

        public void setRegions(Set<String> regions) {
            // NB should already be lower-cased
            this.regions = Collections.unmodifiableSet(new HashSet<String>(regions));
        }

        public Set<String> getRegions() {
            return regions;
        }

        public String getWorld() {
            return world;
        }

        public Set<String> getGroups() {
            return groups;
        }

        public void setWorld(String world) {
            if (world == null)
                throw new IllegalArgumentException("world cannot be null");
            this.world = world;
        }

        public void setGroups(Set<String> groups) {
            this.groups = new HashSet<String>(groups.size());
            for (String group : groups) {
                this.groups.add(group.toLowerCase());
            }
            this.groups = Collections.unmodifiableSet(this.groups);
        }

    }

}
