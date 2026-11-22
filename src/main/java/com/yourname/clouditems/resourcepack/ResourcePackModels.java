package com.yourname.clouditems.resourcepack;

import org.bukkit.NamespacedKey;

/**
 * Item model ids used by the CloudPack resource pack.
 *
 * <p>These ids are set through the {@code item_model} component on plugin-created
 * items only. The resource pack defines them under the {@code clouditems}
 * namespace, so vanilla item textures and vanilla item definitions are never
 * overwritten.</p>
 */
public final class ResourcePackModels {

    public static final String NAMESPACE = "clouditems";

    /** assets/clouditems/items/rune.json */
    public static final NamespacedKey RUNE = new NamespacedKey(NAMESPACE, "rune");

    /** assets/clouditems/items/totem.json */
    public static final NamespacedKey TOTEM = new NamespacedKey(NAMESPACE, "totem");

    private ResourcePackModels() {
    }
}
