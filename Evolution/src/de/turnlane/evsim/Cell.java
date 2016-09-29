package de.turnlane.evsim;

import java.util.Random;

import javafx.scene.paint.Color;

public class Cell {
	
	public static final int MAX_HEALTH = 1000;
	public static final int MAX_STRENGTH = 1000;
	
	public static final int COMMAND_NOTHING = 0;
	public static final int COMMAND_INCREMENT_TOUGHNESS = 1;
	public static final int COMMAND_INCREMENT_STRENGTH = 2;
	public static final int COMMAND_DECREMENT_TOUGHNESS = 3;
	public static final int COMMAND_DECREMENT_STRENGTH = 4;
	public static final int COMMAND_REPRODUCE = 5;
	
	private byte[] m_dna;
	
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
	}
	
	public void setHealth(int health) {
		m_health = health;
		if(m_health > MAX_HEALTH) m_health = MAX_HEALTH;
	}
	
	public int getHealth() {
		return m_health;
	}
	
	public void setStrength(int strength) {
		m_strength = strength;
		if(m_strength > MAX_STRENGTH) m_strength = MAX_STRENGTH;
	}
	
	public int getStrength() {
		return m_strength;
	}
	
	public void tick(World world, int time, int x, int y) {
		//decrement health
		m_health -= 3;
		m_strength--;
		
		switch(m_dna[time]) {
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
			int ad = world.getUnoccupiedAdjected(x, y);
			if(ad != -1) {
				Random random = new Random();
				byte[] dna = new byte[m_dna.length];
				for(int i = 0; i < dna.length - 1; i++) {
					if(random.nextDouble() > 0.9) {
						dna[i] = (byte) random.nextInt(COMMAND_REPRODUCE + 1);
					} else {
						dna[i] = m_dna[i];
					}
				}
				
				Cell c = new Cell(dna, m_hue, m_species, m_dna.length * 3 + m_health, m_dna.length + m_strength);
				world.setCell(c, ad);
			}
			break;
		default:
		}
	}
	
	public Color getColor() {
		return Color.hsb(m_hue, 0.5 * ((double) m_health / (double) MAX_HEALTH) + 0.5, 0.5 * ((double) m_strength / (double) MAX_STRENGTH) + 0.5);
	}
}
