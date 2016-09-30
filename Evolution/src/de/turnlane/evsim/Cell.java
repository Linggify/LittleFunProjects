package de.turnlane.evsim;

import java.util.ArrayList;
import java.util.Random;

import javafx.scene.paint.Color;

public class Cell {
	
	public static final int MAX_HEALTH = 300;
	public static final int MAX_STRENGTH = 1000;
	
	public static final int COMMAND_NOTHING = 0;
	public static final int COMMAND_INCREMENT_TOUGHNESS = 1;
	public static final int COMMAND_INCREMENT_STRENGTH = 2;
	public static final int COMMAND_DECREMENT_TOUGHNESS = 3;
	public static final int COMMAND_DECREMENT_STRENGTH = 4;
	public static final int COMMAND_REPRODUCE = 5;
	
	private byte[] m_dna;
	private int m_dnaClock;
	
	private double m_hue;
	
	private int m_species;
	private int m_health;
	private int m_strength;
	
	public Cell(byte[] dna, double hue, int species, int health, int strength) {
		m_dna = dna;
		
		m_species = species;
		setHealth(health);
		setStrength(strength);
		
		m_hue = hue;
		m_dnaClock = 0;
	}
	
	public void setHealth(int health) {
		m_health = health;
		if(m_health > MAX_HEALTH) m_health = MAX_HEALTH;
		else if(m_health < 0) m_health = 0;
	}
	
	public int getHealth() {
		return m_health;
	}
	
	public void setStrength(int strength) {
		m_strength = strength;
		if(m_strength > MAX_STRENGTH) m_strength = MAX_STRENGTH;
		else if(m_strength < 0) m_strength = 0;
	}
	
	public int getStrength() {
		return m_strength;
	}
	
	public void tick(World world, int x, int y) {
		
		//decrement health
		setHealth(getHealth() - 2);
		setStrength(getStrength() - 1);
		
//		ArrayList<Cell> neighbors = world.getNeighbors(x, y);
		
		
		switch(m_dna[m_dnaClock]) {
		case COMMAND_NOTHING:
			break;
		case COMMAND_INCREMENT_TOUGHNESS:
			setHealth(getHealth() + 1);
			break;
		case COMMAND_INCREMENT_STRENGTH:
			setStrength(getStrength() + 1);
			break;
		case COMMAND_DECREMENT_TOUGHNESS:
			setHealth(getHealth() - 1);
			break;
		case COMMAND_DECREMENT_STRENGTH:
			setStrength(getStrength() - 1);
			break;
		case COMMAND_REPRODUCE:
			Random random = new Random();
			if(random.nextDouble() > 0.5 * ((double) m_strength / (double) MAX_STRENGTH) + 0.5) break;
			int ad = world.getUnoccupiedAdjected(x, y);
			if(ad != -1) {
				byte[] dna = new byte[m_dna.length];
				for(int i = 0; i < dna.length; i++) {
					if(random.nextDouble() > 0.9) {
						dna[i] = (byte) random.nextInt(COMMAND_REPRODUCE + 1);
					} else {
						dna[i] = m_dna[i];
					}
				}
				
				Cell c = new Cell(dna, m_hue, m_species, m_dna.length + m_health, m_dna.length / 2 + m_strength);
				world.setCell(c, ad);
			}
			break;
		default:
		}
		
		//increment dnaclock
		m_dnaClock++;
		if(m_dnaClock >= m_dna.length) m_dnaClock -= m_dna.length;
	}
	
	public Color getColor() {
		return Color.hsb(m_hue, 0.5 * ((double) m_health / (double) MAX_HEALTH) + 0.5, 0.5 * ((double) m_strength / (double) MAX_STRENGTH) + 0.5);
	}
}
