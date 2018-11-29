package io.github.bananapuncher714.cartographer.core.renderer;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import io.github.bananapuncher714.cartographer.core.api.ChunkLocation;
import io.github.bananapuncher714.cartographer.core.util.BlockUtil;

public class CartographerRenderer extends MapRenderer {
	public static final int SIZE = 1;
	
	volatile boolean RUNNING = true;
	
	Thread renderer;
	
	MapDataCache cache = new MapDataCache( new ChunkDataProvider() {
		@Override
		public ChunkData process( ChunkSnapshot snapshot ) {
			byte[] data = new byte[ 256 ];
			
			for ( int x = 0; x < 16; x++ ) {
				for ( int z = 0; z < 16; z++ ) {
					data[ x + z * 16 ] = ( byte ) BlockUtil.getHighestYAt( snapshot, x, 255, z, null );
				}
			}
			
			return new ChunkData( data );
		}
	} );
	
	Map< ChunkLocation, ChunkSnapshot > chunks = new ConcurrentHashMap< ChunkLocation, ChunkSnapshot >();
	
	Set< ChunkLocation > registered = new HashSet< ChunkLocation >();
	
	Queue< Location > load = new ArrayDeque< Location >();
	Queue< byte[] > maps = new ArrayDeque< byte[] >();
	
	ReentrantLock lock = new ReentrantLock();
	
	public CartographerRenderer() {
		renderer = new Thread( this::run );
		renderer.start();
	}
	
	private void run() {
		while ( RUNNING ) {
			lock.lock();
			Location loc = load.poll();
			if ( loc != null && maps.size() < SIZE ) {
				lock.unlock();
				byte[] data = new byte[ 128 * 128 ];
				for ( int rz = 0; rz < 128; rz++ ) {
					int rzx = 128 * rz;
					for ( int rx = 0; rx < 128; rx++ ) {
						Location renderLoc = loc.clone().add( rx, 0, rz );
						ChunkLocation cLocation = new ChunkLocation( renderLoc );
						
						int xOffset = renderLoc.getBlockX() - ( cLocation.getX() << 4 );
						int zOffset = renderLoc.getBlockZ() - ( cLocation.getZ() << 4 );
						
						ChunkData chunkData = cache.getDataAt( cLocation );

						if ( chunkData != null ) {
							data[ rx + rzx ] = chunkData.getData()[ xOffset + zOffset * 16 ];
						} else {
							ChunkSnapshot snapshot = chunks.get( cLocation );

							if ( snapshot == null ) {
								continue;
							}
							
							cache.process( snapshot );
						
							chunks.remove( cLocation );
						}
					}
				}
				
				lock.lock();
				maps.add( data );
				lock.unlock();
			} else {
				lock.unlock();
				
				try {
					Thread.sleep( 10000 );
				} catch ( InterruptedException e ) {
				}
			}
		}
	}
	
	@Override
	public void render( MapView view, MapCanvas canvas, Player player ) {
		Location location = player.getLocation().subtract( 64, 0, 64 );
		int x = location.getChunk().getX();
		int z = location.getChunk().getZ();
		for ( int xo = 0; xo < 9; xo++) {
			for ( int zo = 0; zo < 9; zo++ ) {
				ChunkLocation cLocation = new ChunkLocation( location.getWorld(), x + xo, z + zo );
				if ( !registered.contains( cLocation ) ) {
					chunks.put( cLocation, cLocation.getChunk().getChunkSnapshot() );
					registered.add( cLocation );
				}
			}
		}
		
		lock.lock();
		if ( load.size() < SIZE ) {
			location.setY( location.getWorld().getMaxHeight() - 1 );
			load.add( location );
		}
		
		byte[] data = maps.poll();
		lock.unlock();
		renderer.interrupt();
		if ( data != null ) {
			for ( int rz = 0; rz < 128; rz++ ) {
				int rzx = 128 * rz;
				for ( int rx = 0; rx < 128; rx++ ) {
					canvas.setPixel( rx, rz, data[ rx + rzx ] );
				}
			}
		}
	}
	
	public void terminate() {
		RUNNING = false;
	}
}
