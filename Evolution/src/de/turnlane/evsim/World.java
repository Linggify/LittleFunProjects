package de.turnlane.evsim;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

public class World {

	private ReentrantLock m_worldLock = new ReentrantLock();
	
	private int m_sizeX;
	private int m_sizeY;
	private Cell[] m_grid;
	
	private int m_dnaLength;
	private int m_dnaTickPos;
	
	public World() {
	}
	
	public void initialize(int sizex, int sizey, int speciesCount, double density, int dnaLength) {
		m_worldLock.lock();
		
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
			
			for(int x = minx; x < maxx; x++) {
				for(int y = miny; y < maxy; y++) {
					if(random.nextDouble() < density) {
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
						
						//create starting cells with dna and 100 health and 100 strength
						int coord = x + y * m_sizeX;
						m_grid[coord] = new Cell(dna, hue, i, 100, 100);
					}
				}
			}
		}
		
		m_dnaTickPos = 0;
		
		m_worldLock.unlock();
	}
	
	public void tick() {
		
		double millis = System.currentTimeMillis();
		
		//increment dna pos one per tick, if at the end of dna start over
		m_dnaTickPos++;
		if(m_dnaTickPos >= m_dnaLength) m_dnaTickPos -= m_dnaLength;
		
		m_worldLock.lock();
		for(int i = 0; i < m_grid.length; i++) {
			if(m_grid[i] != null) m_grid[i].tick(this, m_dnaTickPos, i % m_sizeX, i / m_sizeX);
		}
		m_worldLock.unlock();
		
		System.out.println("World tick lasted " + (System.currentTimeMillis() - millis) + " milliseconds");
	}
	
	public void renderWorldOutput(Canvas canvas) {
		m_worldLock.lock();
		
		PixelWriter writer = canvas.getGraphicsContext2D().getPixelWriter();
		
		for(int i = 0; i < m_grid.length; i++) {
			int x = i % m_sizeX;
			int y = i / m_sizeX;
			Color argb = m_grid[i] != null ? m_grid[i].getColor() : Color.BLACK;
			
			writer.setColor(x, y, argb);
		}
		
		for(int i = 0; i < m_grid.length; i++) {
			if(m_grid[i] != null) {
				if(m_grid[i].getHealth() <= 0) {
					m_grid[i] = null;
				}
			}
		}
		
		m_worldLock.unlock();
	}
	
	public void setCell(Cell c, int id) {
		m_grid[id] = c;
	}
	
	public int getUnoccupiedAdjected(int x, int y) {
		ArrayList<Integer> ens = new ArrayList<>();
		int id = toID(x - 1, y - 1);
		if(id != -1 && m_grid[id] == null) ens.add(id);
		
		id = toID(x, y - 1);
		if(id != -1 && m_grid[id] == null) ens.add(id);

		id = toID(x + 1, y - 1);
		if(id != -1 && m_grid[id] == null) ens.add(id);
		
		id = toID(x - 1, y);
		if(id != -1 && m_grid[id] == null) ens.add(id);
		
		id = toID(x + 1, y);
		if(id != -1 && m_grid[id] == null) ens.add(id);
		
		id = toID(x - 1, y + 1);
		if(id != -1 && m_grid[id] == null) ens.add(id);
		
		id = toID(x, y + 1);
		if(id != -1 && m_grid[id] == null) ens.add(id);
		
		id = toID(x + 1, y + 1);
		if(id != -1 && m_grid[id] == null) ens.add(id);
		
		if(!ens.isEmpty()) return ens.get((int) (new Random().nextDouble() * ens.size()));
		return -1;
	}
	
	private int toID(int x, int y) {
		if(x >= 0 && x < m_sizeX && y >= 0 && y < m_sizeY) return x + y * m_sizeX;
		return -1;
	}
}
