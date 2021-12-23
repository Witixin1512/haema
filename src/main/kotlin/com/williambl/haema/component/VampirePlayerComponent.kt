package com.williambl.haema.component

import com.williambl.haema.ability.AbilityModule
import com.williambl.haema.ability.VampireAbility
import dev.onyxstudios.cca.api.v3.component.CopyableComponent
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

@Suppress("UnstableApiUsage")
class VampirePlayerComponent(player: Entity) : VampireComponent, AutoSyncedComponent, CopyableComponent<VampirePlayerComponent> {
    private val syncCallback = { _: KProperty<*>, _: Any?, _: Any? ->
        if (!player.world.isClient) {
            VampireComponent.entityKey.sync(player)
        }
    }

    override var isVampire: Boolean by Delegates.observable(false, syncCallback)
    override var isPermanentVampire: Boolean by Delegates.observable(false, syncCallback)
    override var isKilled: Boolean by Delegates.observable(false, syncCallback)

    override var abilities: MutableMap<VampireAbility, Int> = mutableMapOf(
        AbilityModule.STRENGTH to 1,
        AbilityModule.DASH to 1,
        AbilityModule.INVISIBILITY to 0,
        AbilityModule.IMMORTALITY to 1,
        AbilityModule.VISION to 1
    )

    override var ritualsUsed: MutableSet<Identifier> = mutableSetOf()

    override fun writeToNbt(tag: NbtCompound) {
        tag.putBoolean("isVampire", isVampire)
        tag.putBoolean("isPermanentVampire", isPermanentVampire)
        tag.putBoolean("isKilled", isKilled)
        tag.put("abilities", NbtCompound().also { abilitiesTag -> abilities.forEach { (ability, value) -> abilitiesTag.putInt(AbilityModule.ABILITY_REGISTRY.getId(ability).toString(), value) } })
        tag.put("ritualsUsed", NbtCompound().also {
            it.putInt("Length", ritualsUsed.size)
            ritualsUsed.forEachIndexed { idx, id -> it.putString(idx.toString(), id.toString()) }
        })
    }

    override fun readFromNbt(tag: NbtCompound) {
        isVampire = tag.getBoolean("isVampire")
        isPermanentVampire = tag.getBoolean("isPermanentVampire")
        isKilled = tag.getBoolean("isKilled")
        val abilitiesTag = tag.getCompound("abilities")
        abilitiesTag.fixAbilityData()
        AbilityModule.ABILITY_REGISTRY.entries.filter { abilitiesTag.contains(it.key.value.toString()) }.forEach { abilities[it.value] = abilitiesTag.getInt(it.key.value.toString()) }
        val ritualsUsedTag = tag.getCompound("ritualsUsed")
        ritualsUsed = List(ritualsUsedTag.getInt("Length")) { idx -> Identifier(ritualsUsedTag.getString(idx.toString())) }.toMutableSet()
    }

    override fun copyFrom(other: VampirePlayerComponent) {
        isVampire = other.isVampire
        isPermanentVampire = other.isPermanentVampire
        abilities = other.abilities
        ritualsUsed = other.ritualsUsed
    }

    private fun NbtCompound.fixAbilityData() {
        fun fixAbility(oldName: String, ability: VampireAbility) {
            val newName = AbilityModule.ABILITY_REGISTRY.getId(ability).toString()
            if (this.contains(oldName) && !this.contains(newName)) {
                this.putInt(newName, this.getInt(oldName))
                this.remove(oldName)
            }
        }

        fixAbility("NONE", AbilityModule.STRENGTH)
        fixAbility("STRENGTH", AbilityModule.STRENGTH)
        fixAbility("DASH", AbilityModule.STRENGTH)
        fixAbility("INVISIBILITY", AbilityModule.STRENGTH)
        fixAbility("IMMORTALITY", AbilityModule.STRENGTH)
        fixAbility("VISION", AbilityModule.STRENGTH)
    }
}