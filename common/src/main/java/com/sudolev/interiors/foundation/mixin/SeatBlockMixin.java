package com.sudolev.interiors.foundation.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;

import com.llamalad7.mixinextras.sugar.Local;
import com.sudolev.interiors.content.block.chair.BigChairBlock;
import com.sudolev.interiors.content.entity.BigSeatEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SeatBlock.class)
public abstract class SeatBlockMixin {

	@Inject(method = "isSeatOccupied", at = @At("HEAD"), cancellable = true)
	private static void weCountAsSeatsToo(Level world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if(!world.getEntitiesOfClass(BigSeatEntity.class, new AABB(pos)).isEmpty())
			cir.setReturnValue(true);
	}

	@Redirect(method = "sitDown", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lcom/simibubi/create/content/contraptions/actors/seat/SeatEntity;"))
	private static SeatEntity createCorrectSeatEntity(Level world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() instanceof BigChairBlock
			   ? new BigSeatEntity(world, pos)
			   : new SeatEntity(world, pos);
	}

	@Inject(method = "sitDown", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
	private static void getFixedY(Level world, BlockPos pos, Entity entity, CallbackInfo ci, @Local SeatEntity seat) {
		if(seat instanceof BigSeatEntity) {
			seat.setPos(seat.getX(), seat.getY() + .34f, seat.getZ());
		}
	}
}
