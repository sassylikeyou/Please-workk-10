package com.example.server.template

import com.example.server.ServerType

data class ServerTemplate(
    val id: String,
    val name: String,
    val description: String,
    val serverType: ServerType,
    val defaultMemoryMb: Int,
    val requiredPlugins: List<PluginInfo> = emptyList()
)

data class PluginInfo(
    val name: String,
    val downloadUrl: String
)

object TemplateRegistry {
    val JAVA_PAPER = ServerTemplate(
        id = "java_paper",
        name = "PaperMC",
        description = "High performance Java server",
        serverType = ServerType.JAVA_PAPER,
        defaultMemoryMb = 1024
    )
    
    val JAVA_PURPUR = ServerTemplate(
        id = "java_purpur",
        name = "Purpur",
        description = "Drop-in replacement for Paper with more features",
        serverType = ServerType.JAVA_PURPUR,
        defaultMemoryMb = 1024
    )
    
    val JAVA_FABRIC = ServerTemplate(
        id = "java_fabric",
        name = "FabricMC",
        description = "Lightweight modding toolchain",
        serverType = ServerType.JAVA_FABRIC,
        defaultMemoryMb = 1536
    )

    val JAVA_FORGE = ServerTemplate(
        id = "java_forge",
        name = "Forge",
        description = "Traditional modding platform",
        serverType = ServerType.JAVA_FORGE,
        defaultMemoryMb = 2048
    )

    val JAVA_NEOFORGE = ServerTemplate(
        id = "java_neoforge",
        name = "NeoForge",
        description = "Modern modding platform",
        serverType = ServerType.JAVA_FORGE,
        defaultMemoryMb = 2048
    )

    val BEDROCK_NUKKIT = ServerTemplate(
        id = "bedrock_nukkit",
        name = "Nukkit",
        description = "Native Bedrock server software",
        serverType = ServerType.BEDROCK_NUKKIT,
        defaultMemoryMb = 600
    )

    val BEDROCK_POWER_NUKKIT = ServerTemplate(
        id = "bedrock_power_nukkit",
        name = "PowerNukkit",
        description = "Native Bedrock server software with advanced features",
        serverType = ServerType.BEDROCK_NUKKIT,
        defaultMemoryMb = 600
    )

    val BEDROCK_POWER_NUKKIT_X = ServerTemplate(
        id = "bedrock_power_nukkit_x",
        name = "PowerNukkitX",
        description = "Advanced Bedrock server software",
        serverType = ServerType.BEDROCK_NUKKIT,
        defaultMemoryMb = 600
    )

    val BEDROCK_POCKETMINE_MP = ServerTemplate(
        id = "bedrock_pocketmine_mp",
        name = "PocketMine-MP",
        description = "Native Bedrock server software",
        serverType = ServerType.BEDROCK_NUKKIT,
        defaultMemoryMb = 600
    )

    val BEDROCK_CLOUDBURST_NUKKIT = ServerTemplate(
        id = "bedrock_cloudburst_nukkit",
        name = "Cloudburst Nukkit",
        description = "Native Bedrock server software",
        serverType = ServerType.BEDROCK_NUKKIT,
        defaultMemoryMb = 600
    )

    val BEDROCK_NUKKIT_MOT = ServerTemplate(
        id = "bedrock_nukkit_mot",
        name = "Nukkit-MOT",
        description = "Native Bedrock server software",
        serverType = ServerType.BEDROCK_NUKKIT,
        defaultMemoryMb = 600
    )

    val CROSSPLAY_GEYSER = ServerTemplate(
        id = "crossplay_geyser",
        name = "Crossplay SMP",
        description = "Play with both Java and Bedrock players",
        serverType = ServerType.CROSSPLAY,
        defaultMemoryMb = 1536,
        requiredPlugins = listOf(
            PluginInfo("Geyser", "https://example.com/Geyser.jar"),
            PluginInfo("Floodgate", "https://example.com/Floodgate.jar")
        )
    )
    
    val JAVA_TEMPLATES = listOf(JAVA_PAPER, JAVA_PURPUR, JAVA_FABRIC, JAVA_FORGE, JAVA_NEOFORGE)
    val BEDROCK_TEMPLATES = listOf(
        BEDROCK_NUKKIT,
        BEDROCK_POWER_NUKKIT,
        BEDROCK_POWER_NUKKIT_X,
        BEDROCK_POCKETMINE_MP,
        BEDROCK_CLOUDBURST_NUKKIT,
        BEDROCK_NUKKIT_MOT
    )
    val CROSSPLAY_TEMPLATES = listOf(CROSSPLAY_GEYSER)

    val ALL_TEMPLATES = JAVA_TEMPLATES + BEDROCK_TEMPLATES + CROSSPLAY_TEMPLATES
}
