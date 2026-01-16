package fansirsqi.xposed.sesame.task.manualtask

import fansirsqi.xposed.sesame.task.antForest.AntForest
import fansirsqi.xposed.sesame.task.antFarm.AntFarm
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 手动任务执行器
 */
object ManualTask {

    /**
     * 手动任务流总开关
     */
    @Volatile
    var isManualEnabled = true

    /**
     * 标记手动任务是否正在运行，用于与自动任务互斥
     */
    @Volatile
    var isManualRunning = false
        private set

    /**
     * 为 Java 提供的非 suspend 启动接口
     */
    @JvmStatic
    @JvmOverloads
    fun runSingle(task: FarmSubTask, extraParams: Map<String, Any> = emptyMap()) {
        GlobalThreadPools.execute {
            run(listOf(task), extraParams)
        }
    }

    /**
     * 顺序执行选中的庄园子任务
     */
    suspend fun run(tasks: List<FarmSubTask>, extraParams: Map<String, Any> = emptyMap()) {
        if (!isManualEnabled) {
            Log.record("ManualTask", "⚠️ 手动任务流总开关已关闭，无法执行")
            return
        }

        if (tasks.isEmpty()) {
            Log.record("ManualTask", "⚠️ 未选中任何子任务")
            return
        }

        if (isManualRunning) {
            Log.record("ManualTask", "⚠️ 手动任务已在运行中，请勿重复启动")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                isManualRunning = true
                Log.record("ManualTask", "🚀 开始执行手动任务序列...")
                
                val antForest = AntForest.instance
                val antFarm = AntFarm.instance

                for (task in tasks) {
                    try {
                        Log.record("ManualTask", "⏳ 正在执行: ${task.displayName}...")
                        when (task) {
                            FarmSubTask.FOREST_WHACK_MOLE -> {
                                val mode = extraParams["whackMoleMode"] as? Int ?: 1
                                val games = extraParams["whackMoleGames"] as? Int ?: 5
                                antForest?.manualWhackMole(mode, games)
                            }
                            FarmSubTask.FARM_SEND_BACK_ANIMAL -> antFarm?.manualSendBackAnimal()
                            FarmSubTask.FARM_GAME_LOGIC -> antFarm?.manualFarmGameLogic()
                            FarmSubTask.FARM_CHOUCHOULE -> antFarm?.manualChouChouLeLogic()
                            FarmSubTask.FARM_SPECIAL_FOOD -> {
                                val count = extraParams["specialFoodCount"] as? Int ?: 0
                                antFarm?.manualUseSpecialFood(count)
                            }
                            FarmSubTask.FARM_USE_TOOL -> {
                                val toolType = extraParams["toolType"] as? String ?: ""
                                val toolCount = extraParams["toolCount"] as? Int ?: 1
                                antFarm?.manualUseFarmTool(toolType, toolCount)
                            }
                        }
                    } catch (t: Throwable) {
                        Log.record("ManualTask", "❌ 执行 ${task.displayName} 出错: ${t.message}")
                        Log.printStackTrace(t)
                    }
                }
                Log.record("ManualTask", "✅ 手动任务执行完毕")
            } finally {
                isManualRunning = false
            }
        }
    }
}