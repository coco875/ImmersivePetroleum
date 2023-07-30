package flaxbeard.immersivepetroleum.common.redstone;

import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import blusunrize.immersiveengineering.api.wires.redstone.CapabilityRedstoneNetwork.RedstoneBundleConnection;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

/**
 * @author TwistedGate
 */
public class SimpleRedstoneBundleConnection<M extends PoweredMultiblockBlockEntity<M, ?>> extends RedstoneBundleConnection{
	private M mb;
	private final Supplier<Direction> facing;
	/** From Network */
	private byte[] inputs;
	/** To Network */
	private byte[] outputs;
	public SimpleRedstoneBundleConnection(@Nonnull M mb, @Nullable Direction direction){
		this(mb, () -> direction);
	}
	public SimpleRedstoneBundleConnection(@Nonnull M mb, Supplier<Direction> sup){
		Objects.requireNonNull(mb, "Multiblock BlockEntity must not be null.");
		
		this.mb = mb;
		this.facing = sup;
	}
	
	/**
	 * Called when the RedstoneWireNetwork updates its RS input values.<br>
	 * <br>
	 * Use {@link SimpleRedstoneBundleConnection#setOutput(DyeColor, int)} to set the signal strength of the given color.
	 */
	public void updateNetworkInput(){
	}
	
	/**
	 * Called whenever the RedstoneWireNetwork is changed in some way (both adding/removing connectors and changes in RS values).<br>
	 * <br>
	 * Use {@link SimpleRedstoneBundleConnection#getInput(DyeColor)} to get the current signal strength of the given color.
	 */
	public void onNetworkChange(){
	}
	
	/**
	 * Set the signal strength of the selected color. Must only be called inside {@link #updateNetworkInput()}
	 * 
	 * @param color    to change the signal strength of
	 * @param strength Signal strength to set (0 - 15)
	 * @return true when the signal was changed to the specified strength, false if the network strength is greater than specified
	 */
	protected final boolean setOutput(@Nonnull DyeColor color, int strength){
		if(color == null)
			return false;
		
		if(this.outputs == null)
			throw new IllegalStateException("setOutput must only be called inside updateNetworkInput.");
		
		byte v = (byte) (strength & 0xF);
		if(this.outputs[color.getId()] < v){
			this.outputs[color.getId()] = v;
			markDirty();
			return true;
		}else{
			return false;
		}
	}
	
	protected final int getOutput(@Nonnull DyeColor color){
		if(this.outputs == null)
			throw new IllegalStateException("getOutput must only be called inside updateNetworkInput.");
		
		return ((int) this.outputs[color.getId()]) & 0xF;
	}
	
	/**
	 * Must only be called inside {@link #onNetworkChange()}
	 */
	protected final int getInput(@Nonnull DyeColor color){
		if(color == null)
			return -1;
		if(this.inputs == null)
			throw new IllegalStateException("getInput must be only called inside onNetworkChange.");
		
		return ((int) this.inputs[color.getId()]) & 0xF;
	}
	
	/** @deprecated -> {@link SimpleRedstoneBundleConnection#onNetworkChange()} */
	@Override
	@Deprecated
	public void onChange(byte[] externalInputs, Direction side){
		if(this.facing.get() == null || side == this.facing.get()){
			makeSureItsMaster();
			this.inputs = externalInputs;
			onNetworkChange();
			this.inputs = null;
		}
	}
	
	/** @deprecated -> {@link SimpleRedstoneBundleConnection#updateNetworkInput()} */
	@Override
	@Deprecated
	public void updateInput(byte[] signals, Direction side){
		if(this.facing.get() == null || side == this.facing.get()){
			makeSureItsMaster();
			this.outputs = signals;
			updateNetworkInput();
			this.outputs = null;
		}
	}
	
	@Nonnull
	public final M getMaster(){
		return this.mb;
	}
	
	/**
	 * Found mb.master() to be faulty during construction, hence why im doing this here instead!<br>
	 * Being called at the start in {@link #onChange(byte[], Direction)} and {@link #updateInput(byte[], Direction)}
	 */
	private void makeSureItsMaster(){
		if(this.mb.isDummy())
			this.mb = this.mb.master();
	}
}
