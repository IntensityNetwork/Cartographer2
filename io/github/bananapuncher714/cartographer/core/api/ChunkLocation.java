package io.github.bananapuncher714.cartographer.core.api;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;

public class ChunkLocation {
	private int x, z;
	private String worldName;
	private World world;
	
	public ChunkLocation( Location location ) {
		this( location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4 );
	}
	
	public ChunkLocation( World world, int x, int z ) {
		this.x = x;
		this.z = z;
		this.worldName = world.getName();
		this.world = world;
		
	}
	
	public ChunkLocation( Chunk chunk ) {
		world = chunk.getWorld();
		x = chunk.getX();
		z = chunk.getZ();
	}
	
	public ChunkLocation( ChunkSnapshot snapshot ) {
		worldName = snapshot.getWorldName();
		x = snapshot.getX();
		z = snapshot.getZ();
	}

	public int getX() {
		return x;
	}

	public void setX( int x ) {
		this.x = x;
	}

	public int getZ() {
		return z;
	}

	public void setZ( int z ) {
		this.z = z;
	}

	public World getWorld() {
		if ( world == null ) {
			world = Bukkit.getWorld( worldName );
		}
		return world;
	}

	public void setWorld( World world ) {
		this.world = world;
	}
	
	public Chunk getChunk() {
		return world.getChunkAt( x, z );
	}

	@Override
	public int hashCode() {
		final int prime = 797161;
		int result = 1;
		result = prime * result + ( ( worldName == null ) ? 0 : worldName.hashCode() );
		result = prime * result + x;
		result = prime * result + z;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		
		ChunkLocation other = ( ChunkLocation ) obj;
		
		if ( worldName == null ) {
			if ( other.worldName != null ) {
				return false;
			}
		} else if ( !worldName.equals( other.worldName ) ) {
			return false;
		}
		
		if ( x != other.x ) {
			return false;
		}
		if ( z != other.z ) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "ChunkLocation_x=" + x + "_z=" + z + "_world=" + worldName;
	}
}
