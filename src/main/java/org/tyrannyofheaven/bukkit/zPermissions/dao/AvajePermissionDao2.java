/*
 * Copyright 2012 Allan Saddi <allan@saddi.com>
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
package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

import com.avaje.ebean.EbeanServer;

/**
 * Avaje PermissionDao implementation that keeps everything in memory.
 * 
 * @author asaddi
 */
public class AvajePermissionDao2 extends BaseMemoryPermissionDao {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final EbeanServer ebeanServer;

    private final Executor executor;

    public AvajePermissionDao2(EbeanServer ebeanServer, Executor executor) {
        this.ebeanServer = ebeanServer;
        this.executor = executor != null ? executor : new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private EbeanServer getEbeanServer() {
        return ebeanServer;
    }

    private Executor getExecutor() {
        return executor;
    }

    @Override
    synchronized PermissionRegion getRegion(String region) {
        return super.getRegion(region);
    }

    @Override
    synchronized PermissionWorld getWorld(String world) {
        return super.getWorld(world);
    }

    @Override
    synchronized public Boolean getPermission(String name, boolean group, String region, String world, String permission) {
        return super.getPermission(name, group, region, world, permission);
    }

    @Override
    synchronized public void setPermission(String name, boolean group, String region, String world, String permission, boolean value) {
        super.setPermission(name, group, region, world, permission, value);
    }

    @Override
    synchronized public boolean unsetPermission(String name, boolean group, String region, String world, String permission) {
        return super.unsetPermission(name, group, region, world, permission);
    }

    @Override
    synchronized public void addMember(String groupName, String member, Date expiration) {
        super.addMember(groupName, member, expiration);
    }

    @Override
    synchronized public boolean removeMember(String groupName, String member) {
        return super.removeMember(groupName, member);
    }

    @Override
    synchronized public List<Membership> getGroups(String member) {
        return super.getGroups(member);
    }

    @Override
    synchronized public List<Membership> getMembers(String group) {
        return super.getMembers(group);
    }

    @Override
    synchronized public PermissionEntity getEntity(String name, boolean group) {
        return super.getEntity(name, group);
    }

    @Override
    synchronized public List<PermissionEntity> getEntities(boolean group) {
        return super.getEntities(group);
    }

    @Override
    synchronized public void setGroup(String playerName, String groupName, Date expiration) {
        super.setGroup(playerName, groupName, expiration);
    }

    @Override
    synchronized public void setParent(String groupName, String parentName) {
        super.setParent(groupName, parentName);
    }

    @Override
    synchronized public void setPriority(String groupName, int priority) {
        super.setPriority(groupName, priority);
    }

    @Override
    synchronized public boolean deleteEntity(String name, boolean group) {
        return super.deleteEntity(name, group);
    }

    @Override
    synchronized public List<String> getAncestry(String groupName) {
        return super.getAncestry(groupName);
    }

    @Override
    synchronized public List<Entry> getEntries(String name, boolean group) {
        return super.getEntries(name, group);
    }

    @Override
    synchronized public boolean createGroup(String name) {
        return super.createGroup(name);
    }

    @Override
    synchronized public List<String> getEntityNames(boolean group) {
        return super.getEntityNames(group);
    }

    @Override
    synchronized public Object getMetadata(String name, boolean group, String metadataName) {
        return super.getMetadata(name, group, metadataName);
    }

    @Override
    synchronized public void setMetadata(String name, boolean group, String metadataName, Object value) {
        super.setMetadata(name, group, metadataName, value);
    }

    @Override
    synchronized public boolean unsetMetadata(String name, boolean group, String metadataName) {
        return super.unsetMetadata(name, group, metadataName);
    }

    @Override
    synchronized public void setParents(String groupName, List<String> parentNames) {
        super.setParents(groupName, parentNames);
    }

    @Override
    protected void createRegion(PermissionRegion region) {
        final String name = region.getName().toLowerCase();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionRegion dbRegion = getEbeanServer().find(PermissionRegion.class).where()
                        .eq("name", name)
                        .findUnique();
                if (dbRegion == null) {
                    dbRegion = new PermissionRegion();
                    dbRegion.setName(name);
                    getEbeanServer().save(dbRegion);
                }
            }
        });
    }

    @Override
    protected void createWorld(PermissionWorld world) {
        final String name = world.getName().toLowerCase();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionWorld dbWorld = getEbeanServer().find(PermissionWorld.class).where()
                        .eq("name", name)
                        .findUnique();
                if (dbWorld == null) {
                    dbWorld = new PermissionWorld();
                    dbWorld.setName(name);
                    getEbeanServer().save(dbWorld);
                }
            }
        });
    }

    @Override
    protected void createEntity(PermissionEntity entity) {
        final String name = entity.getDisplayName();
        final boolean group = entity.isGroup();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String lname = name.toLowerCase();
                PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", lname)
                        .eq("group", group)
                        .findUnique();
                if (dbEntity == null) {
                    dbEntity = new PermissionEntity();
                    dbEntity.setName(lname);
                    dbEntity.setGroup(group);
                    dbEntity.setDisplayName(name);
                    // NB assumes name/group/displayName are only attributes that need saving
                    getEbeanServer().save(dbEntity);
                }
            }
        });
    }

    @Override
    protected void createOrUpdateEntry(Entry entry) {
        final String name = entry.getEntity().getDisplayName();
        final boolean group = entry.getEntity().isGroup();
        final String regionName = entry.getRegion() == null ? null : entry.getRegion().getName();
        final String worldName = entry.getWorld() == null ? null : entry.getWorld().getName();
        final String permission = entry.getPermission().toLowerCase();
        final boolean value = entry.isValue();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (entity == null) {
                    entity = inconsistentEntity(name, group);
                }

                PermissionRegion region = null;
                if (regionName != null) {
                    region = getEbeanServer().find(PermissionRegion.class).where()
                            .eq("name", regionName.toLowerCase())
                            .findUnique();
                    if (region == null) {
                        region = inconsistentRegion(regionName);
                    }
                }
                
                PermissionWorld world = null;
                if (worldName != null) {
                    world = getEbeanServer().find(PermissionWorld.class).where()
                            .eq("name", worldName)
                            .findUnique();
                    if (world == null) {
                        world = inconsistentWorld(worldName);
                    }
                }
                
                Entry dbEntry = getEbeanServer().find(Entry.class).where()
                        .eq("entity", entity)
                        .eq("region", region)
                        .eq("world", world)
                        .eq("permission", permission)
                        .findUnique();
                if (dbEntry == null) {
                    dbEntry = new Entry();
                    dbEntry.setEntity(entity);
                    dbEntry.setRegion(region);
                    dbEntry.setWorld(world);
                    dbEntry.setPermission(permission);
                }
                
                dbEntry.setValue(value);
                getEbeanServer().save(dbEntry);
            }
        });
    }

    @Override
    protected void deleteEntry(Entry entry) {
        final String name = entry.getEntity().getDisplayName();
        final boolean group = entry.getEntity().isGroup();
        final String regionName = entry.getRegion() == null ? null : entry.getRegion().getName();
        final String worldName = entry.getWorld() == null ? null : entry.getWorld().getName();
        final String permission = entry.getPermission();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (entity == null) {
                    databaseInconsistency();
                    return;
                }

                PermissionRegion region = null;
                if (regionName != null) {
                    region = getEbeanServer().find(PermissionRegion.class).where()
                            .eq("name", regionName)
                            .findUnique();
                    if (region == null) {
                        databaseInconsistency();
                        return;
                    }
                }
                
                PermissionWorld world = null;
                if (worldName != null) {
                    world = getEbeanServer().find(PermissionWorld.class).where()
                            .eq("name", worldName)
                            .findUnique();
                    if (world == null) {
                        databaseInconsistency();
                        return;
                    }
                }
                
                Entry dbEntry = getEbeanServer().find(Entry.class).where()
                        .eq("entity", entity)
                        .eq("region", region)
                        .eq("world", world)
                        .eq("permission", permission.toLowerCase())
                        .findUnique();
                if (dbEntry == null) {
                    databaseInconsistency();
                    return;
                }

                getEbeanServer().delete(dbEntry);
            }
        });
    }

    @Override
    protected void createOrUpdateMembership(Membership membership) {
        final String name = membership.getGroup().getDisplayName();
        final String member = membership.getMember().toLowerCase();
        final Date expiration = membership.getExpiration();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent object
                PermissionEntity group = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (group == null) {
                    group = inconsistentEntity(name, true);
                }

                Membership dbMembership = getEbeanServer().find(Membership.class).where()
                        .eq("group", group)
                        .eq("member", member)
                        .findUnique();
                if (dbMembership == null) {
                    dbMembership = new Membership();
                    dbMembership.setGroup(group);
                    dbMembership.setMember(member);
                }
                dbMembership.setExpiration(expiration);
                getEbeanServer().save(dbMembership);
            }
        });
    }

    @Override
    protected void deleteEntity(PermissionEntity entity) {
        final String name = entity.getDisplayName();
        final boolean group = entity.isGroup();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (dbEntity == null) {
                    databaseInconsistency();
                    return;
                }
                
                if (group) {
                    getEbeanServer().delete(getEbeanServer().find(Inheritance.class).where()
                            .eq("child", dbEntity)
                            .findList());
                    getEbeanServer().delete(getEbeanServer().find(Inheritance.class).where()
                            .eq("parent", dbEntity)
                            .findList());
                    // backwards compat
                    for (PermissionEntity child : getEbeanServer().find(PermissionEntity.class).where()
                            .eq("parent", dbEntity)
                            .eq("group", true)
                            .findList()) {
                        child.setParent(null);
                        getEbeanServer().save(child);
                    }
                }

                getEbeanServer().delete(dbEntity);
            }
        });
    }

    @Override
    protected void deleteMembership(Membership membership) {
        final String name = membership.getGroup().getDisplayName();
        final String member = membership.getMember();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent object
                PermissionEntity group = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (group == null) {
                    databaseInconsistency();
                    return;
                }

                Membership dbMembership = getEbeanServer().find(Membership.class).where()
                        .eq("group", group)
                        .eq("member", member.toLowerCase())
                        .findUnique();
                if (dbMembership == null) {
                    databaseInconsistency();
                    return;
                }
                
                getEbeanServer().delete(dbMembership);
            }
        });
    }

    @Override
    protected void setEntityParent(PermissionEntity entity, PermissionEntity parent) {
        final String name = entity.getDisplayName();
        final String parentName = parent == null ? null : parent.getDisplayName();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionEntity dbParent = null;
                if (parentName != null) {
                    dbParent = getEbeanServer().find(PermissionEntity.class).where()
                            .eq("name", parentName.toLowerCase())
                            .eq("group", true)
                            .findUnique();
                    if (dbParent == null) {
                        dbParent = inconsistentEntity(parentName, true);
                    }
                }
                
                PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (dbEntity == null) {
                    dbEntity = inconsistentEntity(name, true);
                }
                
                dbEntity.setParent(dbParent);
                getEbeanServer().save(dbEntity);
            }
        });
    }

    @Override
    protected void createOrUpdateInheritance(Inheritance inheritance) {
        final String childName = inheritance.getChild().getDisplayName();
        final String parentName = inheritance.getParent().getDisplayName();
        final int ordering = inheritance.getOrdering();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity child = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", childName.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (child == null) {
                    child = inconsistentEntity(childName, true);
                }

                PermissionEntity parent = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", parentName.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (parent == null) {
                    parent = inconsistentEntity(parentName, true);
                }
                
                Inheritance dbInheritance = getEbeanServer().find(Inheritance.class).where()
                        .eq("child", child)
                        .eq("parent", parent)
                        .findUnique();
                if (dbInheritance == null) {
                    dbInheritance = new Inheritance();
                    dbInheritance.setChild(child);
                    dbInheritance.setParent(parent);
                }
                dbInheritance.setOrdering(ordering);
                getEbeanServer().save(dbInheritance);
            }
        });
    }

    @Override
    protected void deleteInheritance(Inheritance inheritance) {
        final String childName = inheritance.getChild().getDisplayName();
        final String parentName = inheritance.getParent().getDisplayName();
        
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity child = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", childName.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (child == null) {
                    databaseInconsistency();
                    return;
                }

                PermissionEntity parent = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", parentName.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (parent == null) {
                    databaseInconsistency();
                    return;
                }
                
                Inheritance dbInheritance = getEbeanServer().find(Inheritance.class).where()
                        .eq("child", child)
                        .eq("parent", parent)
                        .findUnique();
                if (dbInheritance == null) {
                    databaseInconsistency();
                    return;
                }
                
                getEbeanServer().delete(dbInheritance);
            }
        });
    }

    @Override
    protected void setEntityPriority(PermissionEntity entity, final int priority) {
        final String name = entity.getDisplayName();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", true)
                        .findUnique();
                if (dbEntity == null) {
                    dbEntity = inconsistentEntity(name, true);
                }

                dbEntity.setPriority(priority);
                getEbeanServer().save(dbEntity);
            }
        });
    }

    @Override
    protected void deleteRegions(Collection<PermissionRegion> regions) {
        final Set<String> regionNames = new HashSet<String>(regions.size());
        for (PermissionRegion region : regions) {
            regionNames.add(region.getName());
        }

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                boolean inconsistent = false;

                List<PermissionRegion> dbRegions = new ArrayList<PermissionRegion>(regionNames.size());
                for (String regionName : regionNames) {
                    PermissionRegion dbRegion = getEbeanServer().find(PermissionRegion.class).where()
                            .eq("name", regionName.toLowerCase())
                            .findUnique();
                    if (dbRegion == null)
                        inconsistent = true;
                    else
                        dbRegions.add(dbRegion);
                }

                if (inconsistent)
                    databaseInconsistency();

                if (!dbRegions.isEmpty())
                    getEbeanServer().delete(dbRegions);
            }
        });
    }

    @Override
    protected void deleteWorlds(Collection<PermissionWorld> worlds) {
        final Set<String> worldNames = new HashSet<String>(worlds.size());
        for (PermissionWorld world : worlds) {
            worldNames.add(world.getName());
        }

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                boolean inconsistent = false;

                List<PermissionWorld> dbWorlds = new ArrayList<PermissionWorld>(worldNames.size());
                for (String worldName : worldNames) {
                    PermissionWorld dbWorld = getEbeanServer().find(PermissionWorld.class).where()
                            .eq("name", worldName.toLowerCase())
                            .findUnique();
                    if (dbWorld == null)
                        inconsistent = true;
                    else
                        dbWorlds.add(dbWorld);
                }

                if (inconsistent)
                    databaseInconsistency();

                if (!dbWorlds.isEmpty())
                    getEbeanServer().delete(dbWorlds);
            }
        });
    }

    @Override
    protected void createOrUpdateMetadata(EntityMetadata metadata) {
        final String name = metadata.getEntity().getDisplayName();
        final boolean group = metadata.getEntity().isGroup();
        final String metadataName = metadata.getName().toLowerCase();
        final Object value = metadata.getValue();
        
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (entity == null) {
                    entity = inconsistentEntity(name, group);
                }

                EntityMetadata dbMetadata = getEbeanServer().find(EntityMetadata.class).where()
                        .eq("entity", entity)
                        .eq("name", metadataName)
                        .findUnique();
                if (dbMetadata == null) {
                    dbMetadata = new EntityMetadata();
                    dbMetadata.setEntity(entity);
                    dbMetadata.setName(metadataName);
                }

                dbMetadata.setValue(value);
                getEbeanServer().save(dbMetadata);
            }
        });
    }

    @Override
    protected void deleteMetadata(EntityMetadata metadata) {
        final String name = metadata.getEntity().getDisplayName();
        final boolean group = metadata.getEntity().isGroup();
        final String metadataName = metadata.getName();
        
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Locate dependent objects
                PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                        .eq("name", name.toLowerCase())
                        .eq("group", group)
                        .findUnique();
                if (entity == null) {
                    databaseInconsistency();
                    return;
                }

                EntityMetadata dbMetadata = getEbeanServer().find(EntityMetadata.class).where()
                        .eq("entity", entity)
                        .eq("name", metadataName.toLowerCase())
                        .findUnique();
                if (dbMetadata == null) {
                    databaseInconsistency();
                    return;
                }

                getEbeanServer().delete(dbMetadata);
            }
        });
    }

    public void load() {
        // Current rationale: On any given server, the number of groups will have
        // an upper bound. However, the number of players will not. Granted, most
        // players will simply be members and not full-blown entities themselves.
        // For now, join entities and permissions when fetching players.
        List<PermissionEntity> players = getEbeanServer().createQuery(PermissionEntity.class,
                "find PermissionEntity fetch permissions where group = false")
                .findList();
        // But do not bother for groups.
        List<PermissionEntity> groups = getEbeanServer().createQuery(PermissionEntity.class,
                "find PermissionEntity fetch parent (displayName) where group = true")
                .findList();
        load(players, groups);
    }

    private void load(List<PermissionEntity> players, List<PermissionEntity> groups) {
        MemoryState memoryState = new MemoryState();

        // Create full copies to force lazy-loads
        for (PermissionEntity player : players) {
            PermissionEntity newPlayer = getEntity(memoryState, player.getDisplayName(), false);
            loadPermissions(memoryState, player.getPermissions(), newPlayer);
            loadMetadata(getEbeanServer().find(EntityMetadata.class).where()
                    .eq("entity", player)
                    .findList(), newPlayer);
        }
        for (PermissionEntity group : groups) {
            PermissionEntity newGroup = getEntity(memoryState, group.getDisplayName(), true);
            loadPermissions(memoryState, getEbeanServer().find(Entry.class).where()
                    .eq("entity", group)
                    .findList(), newGroup);
            loadMetadata(getEbeanServer().find(EntityMetadata.class).where()
                    .eq("entity", group)
                    .findList(), newGroup);
            newGroup.setPriority(group.getPriority());
            if (group.getParent() != null) {
                // Backwards compatibility
                PermissionEntity parentEntity = getEntity(memoryState, group.getParent().getDisplayName(), true);

                Inheritance newInheritance = new Inheritance();
                newInheritance.setChild(newGroup);
                newInheritance.setParent(parentEntity);
                newInheritance.setOrdering(0);
                
                // Linkages
                newGroup.getInheritancesAsChild().add(newInheritance);
                parentEntity.getInheritancesAsParent().add(newInheritance);
            }
            else {
                List<Inheritance> inheritances = getEbeanServer().find(Inheritance.class).where()
                        .eq("child", group)
                        .join("parent", "displayName")
                        .findList();
                for (Inheritance inheritance : inheritances) {
                    PermissionEntity parentEntity = getEntity(memoryState, inheritance.getParent().getDisplayName(), true);

                    Inheritance newInheritance = new Inheritance();
                    newInheritance.setChild(newGroup);
                    newInheritance.setParent(parentEntity);
                    newInheritance.setOrdering(inheritance.getOrdering());
                    
                    // Linkages
                    newGroup.getInheritancesAsChild().add(newInheritance);
                    parentEntity.getInheritancesAsParent().add(newInheritance);
                }
            }
            List<Membership> memberships = getEbeanServer().find(Membership.class).where()
                    .eq("group", group)
                    .findList();
            for (Membership membership : memberships) {
                Membership newMembership = new Membership();
                newMembership.setMember(membership.getMember().toLowerCase());
                newMembership.setGroup(newGroup);
                newMembership.setExpiration(membership.getExpiration());
                newGroup.getMemberships().add(newMembership);
                
                rememberMembership(memoryState, newMembership);
            }
        }
        
        synchronized (this) {
            setMemoryState(memoryState);
        }
    }

    private void loadPermissions(MemoryState memoryState, Collection<Entry> permissions, PermissionEntity entity) {
        for (Entry entry : permissions) {
            Entry newEntry = new Entry();

            newEntry.setRegion(entry.getRegion() == null ? null : getRegion(memoryState, entry.getRegion().getName()));
            newEntry.setWorld(entry.getWorld() == null ? null : getWorld(memoryState, entry.getWorld().getName()));
            newEntry.setPermission(entry.getPermission().toLowerCase());
            newEntry.setValue(entry.isValue());

            newEntry.setEntity(entity);
            entity.getPermissions().add(newEntry);
        }
    }

    private void loadMetadata(Collection<EntityMetadata> metadata, PermissionEntity entity) {
        for (EntityMetadata em : metadata) {
            EntityMetadata newMetadata = new EntityMetadata();

            newMetadata.setName(em.getName().toLowerCase());
            newMetadata.setValue(em.getValue());

            newMetadata.setEntity(entity);
            entity.getMetadata().add(newMetadata);
        }
        
        entity.updateMetadataMap();
    }

    private void databaseInconsistency() {
        logger.log(Level.WARNING, "Possible database inconsistency detected; please do a /permissions refresh");
    }

    private PermissionEntity inconsistentEntity(String name, boolean group) {
        databaseInconsistency();
        PermissionEntity entity = new PermissionEntity();
        entity.setName(name.toLowerCase());
        entity.setGroup(group);
        entity.setDisplayName(name);
        getEbeanServer().save(entity);
        return entity;
    }

    private PermissionRegion inconsistentRegion(String name) {
        databaseInconsistency();
        PermissionRegion region = new PermissionRegion();
        region.setName(name.toLowerCase());
        getEbeanServer().save(region);
        return region;
    }

    private PermissionWorld inconsistentWorld(String name) {
        databaseInconsistency();
        PermissionWorld world = new PermissionWorld();
        world.setName(name.toLowerCase());
        getEbeanServer().save(world);
        return world;
    }

}
