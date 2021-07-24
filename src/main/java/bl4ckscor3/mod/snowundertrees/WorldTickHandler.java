package bl4ckscor3.mod.snowundertrees;

import java.util.Optional;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid=SnowUnderTrees.MODID)
public class WorldTickHandler
{
	@SubscribeEvent
	public static void onWorldTick(WorldTickEvent event)
	{
		if(event.side == LogicalSide.SERVER)
		{
			if(event.phase == Phase.START && event.world.isRaining() && Configuration.CONFIG.enableWhenSnowing.get())
			{
				ServerWorld world = (ServerWorld)event.world;

				world.getChunkSource().chunkMap.getChunks().forEach(chunkHolder -> {
					Optional<Chunk> optional = chunkHolder.getEntityTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left();

					if(optional.isPresent() && world.random.nextInt(16) == 0)
					{
						Chunk chunk = optional.get();
						ChunkPos chunkPos = chunk.getPos();
						int chunkX = chunkPos.getMinBlockX();
						int chunkY = chunkPos.getMinBlockZ();
						BlockPos randomPos = world.getBlockRandomPos(chunkX, 0, chunkY, 15);
						Biome biome = world.getBiome(randomPos);
						boolean biomeDisabled = Configuration.CONFIG.filteredBiomes.get().contains(world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(biome).toString());

						if(!biomeDisabled && world.getBlockState(world.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, randomPos).below()).getBlock() instanceof LeavesBlock)
						{
							BlockPos pos = world.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, randomPos);
							BlockState state = world.getBlockState(pos);

							if(biome.shouldSnow(world, pos) && state.isAir(world, pos))
							{
								BlockPos downPos = pos.below();
								BlockState stateBelow = world.getBlockState(downPos);

								if(stateBelow.isFaceSturdy(world, downPos, Direction.UP))
								{
									world.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());

									if(stateBelow.hasProperty(SnowyDirtBlock.SNOWY))
										world.setBlock(downPos, stateBelow.setValue(SnowyDirtBlock.SNOWY, true), 2);
								}
							}
						}
					}
				});
			}
			else if(event.phase == Phase.END && ModList.get().isLoaded("sereneseasons"))
				SereneSeasonsHandler.tryMeltSnowUnderTrees(event);
		}
	}
}
