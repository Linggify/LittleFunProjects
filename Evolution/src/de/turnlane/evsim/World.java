package de.turnlane.evsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

public class World {

	private ReentrantReadWriteLock m_worldLock = new ReentrantReadWriteLock();
	private ReentrantReadWriteLock[] m_newWorldLocks;
	
	private int m_sizeX;
	private int m_sizeY;
	private Cell[] m_grid;
	private Cell[] m_gridNew;
	
	private int m_dnaLength;
	
	public World() {
	}
	
	public void initialize(int sizex, int sizey, int speciesCount, double density, int dnaLength) {
		m_worldLock.writeLock().lock();
		
		m_sizeX = sizex;
		m_sizeY = sizey;
		m_dnaLength = dnaLength;
		m_grid = new Cell[m_sizeX * m_sizeY];
		
		//maximum size of a colony affected by grid size and colony
		int mprX = (int) (0.25 * (m_sizeX / speciesCount));
		int mprY = (int) (0.25 * (m_sizeY / speciesCount));
		
		Random random = new Random();
		
		//create eachc species
		for(int i = 0; i < speciesCount; i++) {
			//each species has a random color and random start colony
			double hue = random.nextDouble() * 360.0;
			int sx = mprX + random.nextInt(m_sizeX - mprX * 2);
			int sy = mprY + random.nextInt(m_sizeY - mprY * 2);
			
			int minx = sx - mprX;
			int maxx = sx + mprX;
			int miny = sy - mprY;
			int maxy = sy + mprY;
			
			//create dna array
			byte[] dna = new byte[m_dnaLength];
			//reproduce at least once
			dna[m_dnaLength - 1] = Cell.COMMAND_REPRODUCE;
			for(int c = 0; c < m_dnaLength - 1; c++) {
				if(random.nextDouble() > 0.7) {
					dna[i] = (byte) random.nextInt(Cell.COMMAND_REPRODUCE + 1);
				} else {
					dna[i] = Cell.COMMAND_NOTHING;
				}
			}

			for(int x = minx; x < maxx; x++) {
				for(int y = miny; y < maxy; y++) {
					if(random.nextDouble() < density) {
						
						//create starting cells with dna and 100 health and 100 strength
						int coord = x + y * m_sizeX;
						m_grid[coord] = new Cell(dna, hue, i, 100, 100);
					}
				}
			}
		}
		
		m_newWorldLocks = new ReentrantReadWriteLock[m_grid.length];
		for(int i = 0; i < m_newWorldLocks.length; i++) {
			m_newWorldLocks[i] = new ReentrantReadWriteLock();
		}
		
		m_worldLock.writeLock().unlock();
	}
	
	public void tick() {
		
		double millis = System.currentTimeMillis();
		
		ExecutorService service = Executors.newFixedThreadPool(4);
		
		m_gridNew = m_grid.clone();
		
		m_worldLock.readLock().lock();
		for(int i = 0; i < m_grid.length; i++) {
			if(m_grid[i] != null) service.submit(new CellTask(this, m_grid[i], i % m_sizeX, i / m_sizeX));
		}
		m_worldLock.readLock().unlock();
		
		service.shutdown();

		m_worldLock.writeLock().lock();
		m_grid = m_gridNew;
		m_worldLock.writeLock().unlock();
		
		
		m_worldLock.writeLock().lock();
		for(int i = 0; i < m_grid.length; i++) {
			if(m_grid[i] != null) {
				if(m_grid[i].getHealth() <= 0) {
					m_grid[i] = null;
				}
			}
		}
		m_worldLock.writeLock().unlock();
		
		
		System.out.println("World tick lasted " + (System.currentTimeMillis() - millis) + " milliseconds");
	}
	
	public void renderWorldOutput(Canvas canvas) {
		m_worldLock.readLock().lock();
		
		PixelWriter writer = canvas.getGraphicsContext2D().getPixelWriter();
		
		for(int i = 0; i < m_grid.length; i++) {
			int x = i % m_sizeX;
			int y = i / m_sizeX;
			Color argb = m_grid[i] != null ? m_grid[i].getColor() : Color.BLACK;
			
			writer.setColor(x, y, argb);
		}
		
		
		m_worldLock.readLock().unlock();
	}
	
	public ArrayList<Cell> getNeighbors(int x, int y) {
		m_worldLock.readLock().lock();
		ArrayList<Cell> ens = new ArrayList<>();
		int id = toID(x - 1, y - 1);
		if(id != -1 && m_grid[id] != null) ens.add(m_grid[id]);
		
		id = toID(x, y - 1);
		if(id != -1 && m_grid[id] != null) ens.add(m_grid[id]);

		id = toID(x + 1, y - 1);
		if(id != -1 && m_grid[id] != null) ens.add(m_grid[id]);
		
		id = toID(x - 1, y);
		if(id != -1 && m_grid[id] != null) ens.add(m_grid[id]);
		
		id = toID(x + 1, y);
		if(id != -1 && m_grid[id] != null) ens.add(m_grid[id]);
		
		id = toID(x - 1, y + 1);
		if(id != -1 && m_grid[id] != null) ens.add(m_grid[id]);
		
		id = toID(x, y + 1);
		if(id != -1 && m_grid[id] != null) ens.add(m_grid[id]);
		
		id = toID(x + 1, y + 1);
		if(id != -1 && m_grid[id] != null) ens.add(m_grid[id]);
		m_worldLock.readLock().unlock();
		
		return ens;
	}
	
	public void setCell(Cell c, int id) {
		m_newWorldLocks[id].writeLock().lock();
		m_gridNew[id] = c;
		m_newWorldLocks[id].writeLock().lock();
	}
	
	public int getUnoccupiedAdjected(int x, int y) {
		m_worldLock.readLock().lock();
		ArrayList<Integer> ens = new ArrayList<>();
		int id = toID(x - 1, y - 1);
		if(id != -1 && m_gridNew[id] == null) ens.add(id);
		
		id = toID(x, y - 1);
		if(id != -1 && m_gridNew[id] == null) ens.add(id);

		id = toID(x + 1, y - 1);
		if(id != -1 && m_gridNew[id] == null) ens.add(id);
		
		id = toID(x - 1, y);
		if(id != -1 && m_gridNew[id] == null) ens.add(id);
		
		id = toID(x + 1, y);
		if(id != -1 && m_gridNew[id] == null) ens.add(id);
		
		id = toID(x - 1, y + 1);
		if(id != -1 && m_gridNew[id] == null) ens.add(id);
		
		id = toID(x, y + 1);
		if(id != -1 && m_gridNew[id] == null) ens.add(id);
		
		id = toID(x + 1, y + 1);
		if(id != -1 && m_gridNew[id] == null) ens.add(id);
		m_worldLock.readLock().unlock();
		
		if(!ens.isEmpty()) return ens.get((int) (new Random().nextDouble() * ens.size()));
		return -1;
	}
	
	private int toID(int x, int y) {
		if(x >= 0 && x < m_sizeX && y >= 0 && y < m_sizeY) return x + y * m_sizeX;
		return -1;
	}
	
	private class CellTask implements Runnable {

		private World m_world;
		private Cell m_target;
		private int m_x;
		private int m_y;
		
		public CellTask(World world, Cell target, int x, int y) {
			m_world = world;
			m_target = target;
			m_x = x;
			m_y = y;
		}
		
		@Override
		public void run() {
			m_target.tick(m_world, m_x, m_y);
		}
		
	}
}
