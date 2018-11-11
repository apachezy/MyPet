/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.compat.v1_13_R2.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyHorse;
import de.Keyle.MyPet.compat.v1_13_R2.entity.EntityMyPet;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;

import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 1.3965F, height = 1.6F)
public class EntityMyHorse extends EntityMyPet implements IJumpable {

    protected static final DataWatcherObject<Boolean> ageWatcher = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.i);
    protected static final DataWatcherObject<Byte> saddleChestWatcher = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.a);
    protected static final DataWatcherObject<Optional<UUID>> ownerWatcher = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.o);
    private static final DataWatcherObject<Integer> variantWatcher = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> armorWatcher = DataWatcher.a(EntityMyHorse.class, DataWatcherRegistry.b);

    int soundCounter = 0;
    int rearCounter = -1;

    public EntityMyHorse(World world, MyPet myPet) {
        super(EntityTypes.HORSE, world, myPet);
    }

    /**
     * Possible visual horse effects:
     * 4 saddle
     * 8 chest
     * 32 head down
     * 64 rear
     */
    protected void applyVisual(int value, boolean flag) {
        int i = this.datawatcher.get(saddleChestWatcher);
        if (flag) {
            this.datawatcher.set(saddleChestWatcher, (byte) (i | value));
        } else {
            this.datawatcher.set(saddleChestWatcher, (byte) (i & (~value)));
        }
    }

    public boolean attack(Entity entity) {
        boolean flag = false;
        try {
            flag = super.attack(entity);
            if (flag) {
                applyVisual(64, true);
                rearCounter = 10;
                this.makeSound("entity.horse.angry", 1.0F, 1.0F);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    @Override
    protected String getDeathSound() {
        return "entity.horse.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.horse.hurt";
    }

    protected String getLivingSound() {
        return "entity.horse.ambient";
    }


    public boolean handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack)) {
            return true;
        }

        if (itemStack != null && canUseItem()) {
            org.bukkit.inventory.ItemStack is = CraftItemStack.asBukkitCopy(itemStack);
            if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && !getMyPet().isBaby() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setSaddle(is);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (getHorseArmor(is) > 0 && !getMyPet().hasArmor() && !getMyPet().isBaby() && getOwner().getPlayer().isSneaking() && canEquip()) {
                getMyPet().setArmor(is);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                return true;
            } else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
                if (getMyPet().hasArmor()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getArmor()));
                    entityitem.pickupDelay = 10;
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    this.world.addEntity(entityitem);
                }
                if (getMyPet().hasChest()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getChest()));
                    entityitem.pickupDelay = 10;
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    this.world.addEntity(entityitem);
                }
                if (getMyPet().hasSaddle()) {
                    EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY + 1, this.locZ, CraftItemStack.asNMSCopy(getMyPet().getSaddle()));
                    entityitem.pickupDelay = 10;
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    this.world.addEntity(entityitem);
                }

                makeSound("entity.sheep.shear", 1.0F, 1.0F);
                getMyPet().setChest(null);
                getMyPet().setSaddle(null);
                getMyPet().setArmor(null);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }

                return true;
            } else if (Configuration.MyPet.Horse.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.a);
                    }
                }
                getMyPet().setBaby(false);
                return true;
            }
        }
        return false;
    }

    private int getHorseArmor(org.bukkit.inventory.ItemStack itemstack) {
        ItemStack is = CraftItemStack.asNMSCopy(itemstack);
        EnumHorseArmor horseArmor = EnumHorseArmor.a(is);
        return horseArmor.a();
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(ageWatcher, false);
        this.datawatcher.register(saddleChestWatcher, (byte) 0);
        this.datawatcher.register(ownerWatcher, Optional.empty());
        this.datawatcher.register(variantWatcher, 0);
        this.datawatcher.register(armorWatcher, 0);
    }

    protected void initAttributes() {
        super.initAttributes();
        this.getAttributeMap().b(EntityHorseAbstract.attributeJumpStrength);
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(ageWatcher, getMyPet().isBaby());
        this.datawatcher.set(armorWatcher, getHorseArmor(getMyPet().getArmor()));
        this.datawatcher.set(variantWatcher, getMyPet().getVariant());
        applyVisual(4, getMyPet().hasSaddle());
    }

    public void onLivingUpdate() {
        boolean oldRiding = hasRider;
        super.onLivingUpdate();
        if (!hasRider) {
            if (rearCounter > -1 && rearCounter-- == 0) {
                applyVisual(64, false);
                rearCounter = -1;
            }
        }
        if (oldRiding != hasRider) {
            if (hasRider) {
                applyVisual(4, true);
            } else {
                applyVisual(4, getMyPet().hasSaddle());
            }
        }
    }

    @Override
    public void playStepSound(BlockPosition pos, Block block) {
        if (!block.getBlockData().getMaterial().isLiquid()) {
            SoundEffectType soundeffecttype = block.getStepSound();
            if (this.world.getType(pos.up()).getBlock() == Blocks.SNOW_BLOCK) {
                soundeffecttype = Blocks.SNOW_BLOCK.getStepSound();
            }
            if (this.isVehicle()) {
                ++this.soundCounter;
                if (this.soundCounter > 5 && this.soundCounter % 3 == 0) {
                    this.a(SoundEffects.ENTITY_HORSE_GALLOP, soundeffecttype.a() * 0.15F, soundeffecttype.b());
                } else if (this.soundCounter <= 5) {
                    this.a(SoundEffects.ENTITY_HORSE_STEP_WOOD, soundeffecttype.a() * 0.15F, soundeffecttype.b());
                }
            } else if (!block.getBlockData().getMaterial().isLiquid()) {
                this.soundCounter += 1;
                a(SoundEffects.ENTITY_HORSE_STEP_WOOD, soundeffecttype.a() * 0.15F, soundeffecttype.b());
            } else {
                a(SoundEffects.ENTITY_HORSE_STEP, soundeffecttype.a() * 0.15F, soundeffecttype.b());
            }
        }
    }

    public MyHorse getMyPet() {
        return (MyHorse) myPet;
    }

    /* Jump power methods */
    @Override
    public boolean G_() {
        return true;
    }

    @Override
    public void b(int i) {
        this.jumpPower = i;
    }

    @Override
    public void I_() {
    }
}