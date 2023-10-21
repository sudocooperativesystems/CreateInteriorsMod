package com.sudolev.interiors.block.seat;

import java.util.List;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.sudolev.interiors.entity.BigSeatEntity;
import com.sudolev.interiors.registry.CIBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.sudolev.interiors.block.seat.ChairBlock.ArmrestConfiguration.BOTH;
import static com.sudolev.interiors.block.seat.ChairBlock.ArmrestConfiguration.DEFAULT;
import static com.sudolev.interiors.block.seat.ChairBlock.ArmrestConfiguration.LEFT;
import static com.sudolev.interiors.block.seat.ChairBlock.ArmrestConfiguration.NONE;
import static com.sudolev.interiors.block.seat.ChairBlock.ArmrestConfiguration.RIGHT;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChairBlock extends DirectionalSeatBlock implements ProperWaterloggedBlock, IWrenchable {
    protected final DyeColor color;

    public enum ArmrestConfiguration implements StringRepresentable {
        BOTH, NONE, LEFT, RIGHT;

        public static final ArmrestConfiguration DEFAULT = BOTH;

        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }

    public static final EnumProperty<ArmrestConfiguration> ARMRESTS = EnumProperty.create("armrests", ArmrestConfiguration.class);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ChairBlock(Properties properties, DyeColor color) {
        super(properties, color);
        this.color = color;
        registerDefaultState(defaultBlockState()
                .setValue(WATERLOGGED, false)
                .setValue(ARMRESTS, DEFAULT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED).add(FACING).add(ARMRESTS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private static final VoxelShape SHAPE = Shapes.join(
            Block.box(0, 5, 0, 16, 13, 16),
            Block.box(0, 0, 4, 16, 5, 12),
            BooleanOp.OR
    );

    public static void sitDown(Level world, BlockPos pos, Entity entity) {
        if (world.isClientSide)
            return;
        BigSeatEntity seat = new BigSeatEntity(world, pos);
        seat.setPos(pos.getX() + .5f, pos.getY() + .34f, pos.getZ() + .5f);
        world.addFreshEntity(seat);
        entity.startRiding(seat, true);
        if (entity instanceof TamableAnimal ta)
            ta.setInSittingPose(true);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult result) {
        ItemStack heldItem = player.getItemInHand(hand);

		if(heldItem == AllItems.WRENCH.asStack(1)
           || heldItem.is(AllItems.WRENCH.asItem())
           || player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}

		DyeColor color = DyeColor.getColor(heldItem);

        if (color != null && color != this.color) {
            if (world.isClientSide)
                return InteractionResult.SUCCESS;
            BlockState newState = BlockHelper.copyProperties(state, CIBlocks.CHAIRS.get(color).getDefaultState());
            world.setBlockAndUpdate(pos, newState);
            return InteractionResult.SUCCESS;
        }

        List<BigSeatEntity> seats = world.getEntitiesOfClass(BigSeatEntity.class, new AABB(pos));
        if (!seats.isEmpty()) {
            BigSeatEntity BigSeatEntity = seats.get(0);
            List<Entity> passengers = BigSeatEntity.getPassengers();
            if (!passengers.isEmpty() && passengers.get(0) instanceof Player)
                return InteractionResult.PASS;
            if (!world.isClientSide) {
                BigSeatEntity.ejectPassengers();
                player.startRiding(BigSeatEntity);
            }
            return InteractionResult.SUCCESS;
        }

		if(!world.isClientSide) {
			sitDown(world, pos, getLeashed(world, player).or(player));
		}
		return InteractionResult.SUCCESS;
	}

    @Override
    public void updateEntityAfterFallOn(BlockGetter reader, Entity entity) {
        BlockPos pos = entity.blockPosition();
        if (entity instanceof Player || !(entity instanceof LivingEntity) || !canBePickedUp(entity)
                || isSeatOccupied(entity.level(), pos)) {
            if (entity.isSuppressingBounce()) {
                super.updateEntityAfterFallOn(reader, entity);
                return;
            }

            Vec3 vec3 = entity.getDeltaMovement();
            if (vec3.y < 0) {
                double d0 = entity instanceof LivingEntity ? 1 : 0.8;
                entity.setDeltaMovement(vec3.x, -vec3.y * 0.66 * d0, vec3.z);
            }

            return;
        }
        if (reader.getBlockState(pos).getBlock() != this)
            return;
        sitDown(entity.level(), pos, entity);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Vec3 clickPos = pos.getCenter().subtract(context.getClickLocation());

        state = switch(state.getValue(FACING)) {
            case NORTH -> clickPos.x > 0 ? toggleLeft(state) : toggleRight(state);
            case SOUTH -> clickPos.x < 0 ? toggleLeft(state) : toggleRight(state);
            case WEST -> clickPos.z < 0 ? toggleLeft(state) : toggleRight(state);
            case EAST -> clickPos.z > 0 ? toggleLeft(state) : toggleRight(state);
            default -> state;
        };

        if(!world.isClientSide) {
            world.setBlock(pos, state, 3);
        }

        return InteractionResult.SUCCESS;
    }

    private BlockState toggleLeft(BlockState state) {
        return state.setValue(ARMRESTS, switch(state.getValue(ARMRESTS)) {
            case BOTH -> RIGHT;
            case NONE -> LEFT;
            case LEFT -> NONE;
            case RIGHT -> BOTH;
        });
    }

    private BlockState toggleRight(BlockState state) {
        return state.setValue(ARMRESTS, switch(state.getValue(ARMRESTS)) {
            case BOTH -> LEFT;
            case NONE -> RIGHT;
            case LEFT -> BOTH;
            case RIGHT -> NONE;
        });
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if(!world.isClientSide) {
            world.setBlock(pos, state.setValue(ARMRESTS, switch(state.getValue(ARMRESTS)) {
                case BOTH, LEFT, RIGHT -> NONE;
                case NONE -> BOTH;
            }), 3);
        }

        return InteractionResult.SUCCESS;
    }

	public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};

        int times = (to.ordinal() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }

        return buffer[0];
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch(state.getValue(FACING)) {
			case NORTH -> SHAPE;
			case SOUTH -> rotateShape(Direction.NORTH, Direction.WEST, SHAPE);
			case WEST -> rotateShape(Direction.NORTH, Direction.EAST, SHAPE);
			default -> rotateShape(Direction.NORTH, Direction.SOUTH, SHAPE);
		};
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShape(state, level, pos, context);
    }
}