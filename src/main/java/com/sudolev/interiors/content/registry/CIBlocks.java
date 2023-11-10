package com.sudolev.interiors.content.registry;

import static com.simibubi.create.AllInteractionBehaviours.interactionBehaviour;
import static com.simibubi.create.AllMovementBehaviours.movementBehaviour;
import static com.simibubi.create.foundation.block.ProperWaterloggedBlock.WATERLOGGED;
import static com.simibubi.create.foundation.data.TagGen.axeOnly;
import static com.sudolev.interiors.CreateInteriors.REGISTRATE;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.components.actors.SeatInteractionBehaviour;
import com.simibubi.create.content.contraptions.components.actors.SeatMovementBehaviour;
import com.simibubi.create.content.logistics.block.display.AllDisplayBehaviours;
import com.simibubi.create.content.logistics.block.display.source.EntityNameDisplaySource;
import com.simibubi.create.foundation.block.DyedBlockList;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.sudolev.interiors.CreateInteriors;
import com.sudolev.interiors.content.block.WallMountedTable;
import com.sudolev.interiors.content.block.seat.BigChairBlock;
import com.sudolev.interiors.content.block.seat.BigSeatMovementBehaviour;
import com.sudolev.interiors.content.block.seat.ChairBlock;
import com.sudolev.interiors.content.block.seat.DirectionalSeatBlock;
import com.sudolev.interiors.content.block.seat.FloorChairBlock;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.Registry;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.client.model.generators.ConfiguredModel;

@SuppressWarnings("unused")
public final class CIBlocks {

	static {
		REGISTRATE.creativeModeTab(() -> CITab.TAB_INTERIORS);
	}

	public static final BlockEntry<Block> SEATWOOD_PLANKS = REGISTRATE.block("seatwood_planks", Block::new)
		.initialProperties(SharedProperties::wooden)
		.properties(p -> p.color(MaterialColor.TERRACOTTA_ORANGE))
		.transform(axeOnly())
		.tag(BlockTags.PLANKS)
		.item()
		.tag(ItemTags.PLANKS)
		.build()
		.register();

	public static final BlockEntry<WallMountedTable> WALL_MOUNTED_TABLE = REGISTRATE.block("wall_mounted_table", WallMountedTable::new)
		.initialProperties(SharedProperties::wooden)
		.properties(p -> p.color(MaterialColor.TERRACOTTA_ORANGE))
		.transform(axeOnly())
		.blockstate((context, provider) -> provider.getVariantBuilder(context.get())
			.forAllStatesExcept(state -> {
				String facing = state.getValue(ChairBlock.FACING).getSerializedName();
				int rotation = facing(state);

				return ConfiguredModel.builder()
					.modelFile(provider.models().getExistingFile(provider.modLoc("block/wall_table"))).rotationY(rotation)
					.build();
			}, WATERLOGGED))
		.item()
			.model(AssetLookup.customBlockItemModel("wall_table"))
		.build()
		.register();

	public static final DyedBlockList<FloorChairBlock> FLOOR_CHAIRS = new DyedBlockList<>(color -> {
		String colorName = color.getSerializedName();

		return REGISTRATE.block(colorName + "_floor_chair", p -> new FloorChairBlock(p, color))
			.initialProperties(SharedProperties::wooden)
			.properties(p -> p.color(color.getMaterialColor()))
			.transform(axeOnly())
			.blockstate((c, p) -> p.getVariantBuilder(c.get())
				.forAllStatesExcept(state -> {
					String facing = state.getValue(ChairBlock.FACING).getSerializedName();
					String armrest = state.getValue(ChairBlock.ARMRESTS).getSerializedName();
					String cropped_state = state.getValue(ChairBlock.CROPPED_BACK) ? "_cropped" : "";

					int rotation = facing(state);

					ResourceLocation top = Create.asResource("block/seat/top_" + colorName);
					ResourceLocation side = Create.asResource("block/seat/side_" + colorName);
					ResourceLocation sideTop = p.modLoc("block/chair/side_top_" + colorName);

					return ConfiguredModel.builder().modelFile(p.models()
																.withExistingParent("block/floor_chair/" + colorName + "_floor_chair_" + armrest + cropped_state,
																	p.modLoc("block/floor_chair/" + armrest + cropped_state))
																.texture("top", top)
																.texture("side", side)
																.texture("side_front", side)
																.texture("side_top", sideTop))
													.rotationY(rotation)
													.build();
				}, WATERLOGGED))
			.recipe((c, p) -> {
				ShapelessRecipeBuilder.shapeless(c.get())
									  .requires(ItemTags.WOODEN_SLABS)
									  .requires(ItemTags.WOODEN_SLABS)
						.requires(DyeHelper.getWoolOfDye(color))
									  .unlockedBy("has_seat", RegistrateRecipeProvider.has(AllItemTags.SEATS.tag))
									  .save(p, CreateInteriors.asResource("crafting/floor_chair/" + c.getName()));

				ShapelessRecipeBuilder.shapeless(c.get())
									  .requires(ItemTags.WOODEN_SLABS)
									  .requires(AllBlocks.SEATS.get(color).asStack().getItem())
									  .unlockedBy("has_seat", RegistrateRecipeProvider.has(AllItemTags.SEATS.tag))
									  .save(p, CreateInteriors.asResource("crafting/floor_chair/" + c.getName() + "_from_seat"));

				ShapelessRecipeBuilder.shapeless(c.get())
						// .requires(CITags.Items.FLOOR_CHAIRS)
									  .requires(color.getTag())
						// .unlockedBy("has_floor_chair", RegistrateRecipeProvider.has(CITags.Items.FLOOR_CHAIRS))
									  .save(p, CreateInteriors.asResource("crafting/floor_chair/" + c.getName() + "_from_other_floor_chair"));
			})
			.onRegister(movementBehaviour(new SeatMovementBehaviour()))
			.onRegister(interactionBehaviour(new SeatInteractionBehaviour()))
				.onRegister(AllDisplayBehaviours.assignDataBehaviour(new EntityNameDisplaySource(), "entity_name"))
				.onRegisterAfter(Registry.ITEM_REGISTRY, v -> TooltipHelper.referTo(v, "block.interiors.chair"))
				.tag(CITags.BlockTags.FLOOR_CHAIRS.tag)
				.item()
				.tag(CITags.ItemTags.FLOOR_CHAIRS.tag)
				   .model(AssetLookup.customBlockItemModel("floor_chair", colorName + "_floor_chair_" + ChairBlock.ArmrestConfiguration.DEFAULT.getSerializedName()))
				   .build().register();
	});
	public static final DyedBlockList<BigChairBlock> CHAIRS = new DyedBlockList<>(color -> {
		String colorName = color.getSerializedName();

		return REGISTRATE.block(colorName + "_chair", p -> new BigChairBlock(p, color))
			.initialProperties(SharedProperties::wooden)
			.properties(p -> p.color(color.getMaterialColor()))
			.transform(axeOnly())
			.blockstate((c, p) -> p.getVariantBuilder(c.get())
				.forAllStatesExcept(state -> {
					String armrest = state.getValue(ChairBlock.ARMRESTS).getSerializedName();
					String cropped_state = state.getValue(ChairBlock.CROPPED_BACK) ? "_cropped" : "";

					int rotation = facing(state);

					ResourceLocation top = Create.asResource("block/seat/top_" + colorName);
					ResourceLocation side = Create.asResource("block/seat/side_" + colorName);
					ResourceLocation sideTop = p.modLoc("block/chair/side_top_" + colorName);

					return ConfiguredModel.builder().modelFile(p.models()
																.withExistingParent("block/chair/" + colorName + "_chair_" + armrest + cropped_state,
																	p.modLoc("block/chair/" + armrest + cropped_state))
																.texture("top", top).texture("side_top", sideTop)
																.texture("side_front", side).texture("side", side))
													.rotationY(rotation).build();
				}, WATERLOGGED))
			.recipe((c, p) -> {
				ShapelessRecipeBuilder.shapeless(c.get())
									  .requires(ItemTags.WOODEN_SLABS)
									  .requires(ItemTags.PLANKS)
									  .requires(DyeHelper.getWoolOfDye(color))
									  .unlockedBy("has_seat", RegistrateRecipeProvider.has(AllItemTags.SEATS.tag))
									  .save(p, CreateInteriors.asResource("crafting/chair/" + c.getName()));

				ShapelessRecipeBuilder.shapeless(c.get())
									  .requires(ItemTags.PLANKS)
									  .requires(AllBlocks.SEATS.get(color).asStack().getItem())
									  .unlockedBy("has_seat", RegistrateRecipeProvider.has(AllItemTags.SEATS.tag))
									  .save(p, CreateInteriors.asResource("crafting/chair/" + c.getName() + "_from_seat"));
				ShapelessRecipeBuilder.shapeless(c.get())
									  .requires(ItemTags.WOODEN_SLABS)
									  .requires(FLOOR_CHAIRS.get(color).asStack().getItem()) // REMOMVE ItemLike if it breaks stuff!
						// .unlockedBy("has_floor_chair", RegistrateRecipeProvider.has(CITags.Items.FLOOR_CHAIRS))
									  .save(p, CreateInteriors.asResource("crafting/chair/" + c.getName() + "_from_floor_chair"));

				ShapelessRecipeBuilder.shapeless(c.get())
						// .requires(CITags.ItemTags.CHAIRS.tag)
									  .requires(color.getTag())
						// .unlockedBy("has_chair", RegistrateRecipeProvider.has(CITags.ItemTags.CHAIRS.tag))
									  .save(p, CreateInteriors.asResource("crafting/chair/" + c.getName() + "_from_other_chair"));
			})
			.onRegister(movementBehaviour(new BigSeatMovementBehaviour()))
			.onRegister(interactionBehaviour(new SeatInteractionBehaviour()))
				.onRegister(AllDisplayBehaviours.assignDataBehaviour(new EntityNameDisplaySource(), "entity_name"))
				.onRegisterAfter(Registry.ITEM_REGISTRY, v -> TooltipHelper.referTo(v, "block.interiors.chair"))
				.tag(CITags.BlockTags.CHAIRS.tag)
			.item()
				.tag(CITags.ItemTags.CHAIRS.tag)
			.model(AssetLookup.customBlockItemModel("chair", colorName + "_chair_" + ChairBlock.ArmrestConfiguration.DEFAULT.getSerializedName()))
			.build()
			.register();
	});

	public static final BlockEntry<BigChairBlock> KELP_CHAIR = REGISTRATE.block("kelp_chair", p -> new BigChairBlock(p, DyeColor.BLACK))
		.initialProperties(SharedProperties::wooden)
		.properties(p -> p.color(MaterialColor.TERRACOTTA_BLACK))
		.transform(axeOnly())
		.blockstate((c, p) -> p.getVariantBuilder(c.get())
			.forAllStatesExcept(state -> {
				String armrest = state.getValue(ChairBlock.ARMRESTS).getSerializedName();
				String cropped_state = state.getValue(ChairBlock.CROPPED_BACK) ? "_cropped" : "";

				int rotation = facing(state);

				return ConfiguredModel.builder().modelFile(p.models()
													.withExistingParent("block/chair/kelp_chair_" + armrest + cropped_state,
														p.modLoc("block/chair/" + armrest + cropped_state)))
												.rotationY(rotation)
												.build();
			}, WATERLOGGED))
		.onRegister(movementBehaviour(new BigSeatMovementBehaviour()))
		.onRegister(interactionBehaviour(new SeatInteractionBehaviour()))
			.onRegisterAfter(Registry.ITEM_REGISTRY, v -> TooltipHelper.referTo(v, "block.interiors.chair"))
		.item()
		.model(AssetLookup.customBlockItemModel("chair", "kelp_chair_" + ChairBlock.ArmrestConfiguration.DEFAULT.getSerializedName()))
		.build()
		.register();

	public static final BlockEntry<FloorChairBlock> KELP_FLOOR_CHAIR = REGISTRATE.block("kelp_floor_chair", p -> new FloorChairBlock(p, DyeColor.BLACK))
		.initialProperties(SharedProperties::wooden)
		.properties(p -> p.color(MaterialColor.TERRACOTTA_BLACK))
		.transform(axeOnly())
		.blockstate((c, p) -> p.getVariantBuilder(c.get())
			.forAllStatesExcept(state -> {
				String armrest = state.getValue(ChairBlock.ARMRESTS).getSerializedName();
				String cropped_state = state.getValue(ChairBlock.CROPPED_BACK) ? "_cropped" : "";

				int rotation = facing(state);

				return ConfiguredModel.builder().modelFile(p.models()
													.withExistingParent("block/chair/kelp_floor_chair_" + armrest + cropped_state,
														p.modLoc("block/floor_chair/" + armrest + cropped_state)))
												.rotationY(rotation)
												.build();
			}, WATERLOGGED))
		.onRegister(movementBehaviour(new SeatMovementBehaviour()))
		.onRegister(interactionBehaviour(new SeatInteractionBehaviour()))
			.onRegisterAfter(Registry.ITEM_REGISTRY, v -> TooltipHelper.referTo(v, "block.interiors.chair"))
		.item()
		.model(AssetLookup.customBlockItemModel("chair", "kelp_floor_chair_" + ChairBlock.ArmrestConfiguration.DEFAULT.getSerializedName()))
		.build()
		.register();

	public static final BlockEntry<DirectionalSeatBlock> KELP_SEAT = REGISTRATE.block("kelp_seat", p -> new DirectionalSeatBlock(p, DyeColor.BLACK))
		.initialProperties(SharedProperties::wooden)
		.properties(p -> p.color(MaterialColor.TERRACOTTA_BLACK))
		.transform(axeOnly())
		.blockstate((context, provider) -> provider.getVariantBuilder(context.get())
			.forAllStatesExcept(state -> {
				String facing = state.getValue(ChairBlock.FACING).getSerializedName();
				int rotation = facing(state);

				return ConfiguredModel.builder()
					.modelFile(provider.models().getExistingFile(provider.modLoc("block/kelp_seat"))).rotationY(rotation)
					.build();
			}, WATERLOGGED))
		.onRegister(movementBehaviour(new SeatMovementBehaviour()))
		.onRegister(interactionBehaviour(new SeatInteractionBehaviour()))
			.onRegister(AllDisplayBehaviours.assignDataBehaviour(new EntityNameDisplaySource(), "entity_name"))
			.onRegisterAfter(Registry.ITEM_REGISTRY, v -> TooltipHelper.referTo(v, "block.create.brown_seat"))
		.simpleItem()
		.register();

	public static void register() {
		CreateInteriors.LOGGER.info("Registering blocks!");
	}

	private static int facing(BlockState state) {
		return switch(state.getValue(ChairBlock.FACING)) {
			case NORTH -> 0;
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}
}