/*
 * Created on Aug 4, 2005
 */
package ui.hexmap;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.plaf.*;

import game.*;
import game.special.SpecialTileLay;
import ui.*;

/**
 * Base class that stores common info for HexMap independant of Hex
 * orientations.
 */
public abstract class HexMap extends JComponent implements MouseListener,
		MouseMotionListener
{

	// Abstract Methods
	protected abstract void setupHexesGUI();

	// GUI hexes need to be recreated for each object, since scale varies.
	protected GUIHex[][] h;
	MapHex[][] hexArray;
	protected ArrayList hexes;
	// protected ORWindow window;

	protected int scale = 2 * Scale.get();
	protected int cx;
	protected int cy;

	protected static GUIHex selectedHex = null;
	// protected UpgradesPanel upgradesPanel = null;
	protected Dimension preferredSize;

	protected ArrayList extraTileLays = new ArrayList();
	protected ArrayList unconnectedTileLays = new ArrayList();

	public void setupHexes()
	{
		setupHexesGUI();
	}

	/**
	 * Return the GUIBattleHex that contains the given point, or null if none
	 * does.
	 */
	GUIHex getHexContainingPoint(Point2D.Double point)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.contains(point))
			{
				return hex;
			}
		}

		return null;
	}

	GUIHex getHexContainingPoint(Point point)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.contains(point))
			{
				return hex;
			}
		}

		return null;
	}

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		try
		{
			// Abort if called too early.
			Rectangle rectClip = g.getClipBounds();
			if (rectClip == null)
			{
				return;
			}

			Iterator it = hexes.iterator();
			while (it.hasNext())
			{
				GUIHex hex = (GUIHex) it.next();
				Rectangle hexrect = hex.getBounds();

				if (g.hitClip(hexrect.x,
						hexrect.y,
						hexrect.width,
						hexrect.height))
				{
					hex.paint(g);
				}
			}
		}
		catch (NullPointerException ex)
		{
			// If we try to paint before something is loaded, just retry later.
		}
	}

	public Dimension getMinimumSize()
	{
		Dimension dim = new Dimension();
		Rectangle r = ((GUIHex) h[h.length][h[0].length]).getBounds();
		dim.height = r.height + 40;
		dim.width = r.width + 100;
		return dim;
	}

	public Dimension getPreferredSize()
	{
		return preferredSize;
	}

	public void mouseClicked(MouseEvent arg0)
	{
		Point point = arg0.getPoint();
		GUIHex clickedHex = getHexContainingPoint(point);

		if (ORWindow.baseTokenLayingEnabled)
		{
			selectHex(clickedHex);

			// upgradesPanel.setCancelText(Game.getText("Cancel"));
			if (selectedHex != null)
			{
				GameUILoader.orWindow.setSubStep(ORWindow.CONFIRM_TOKEN);
			}
			else
			{
				GameUILoader.orWindow.setSubStep(ORWindow.SELECT_HEX_FOR_TOKEN);
			}
		}
		else if (ORWindow.tileLayingEnabled)
		{
			if (GameUILoader.orWindow.getSubStep() == ORWindow.ROTATE_OR_CONFIRM_TILE
					&& clickedHex == selectedHex)
			{
				selectedHex.rotateTile();
				repaint(selectedHex.getBounds());
			}
			else
			{

				if (selectedHex != null && clickedHex != selectedHex)
				{
					selectedHex.removeTile();
					selectHex(null);
				}
				if (clickedHex != null)
				{
					if (clickedHex.getHexModel().isUpgradeableNow())
					{
						selectHex(clickedHex);
						GameUILoader.orWindow.setSubStep(ORWindow.SELECT_TILE);
					}
					else
					{
						JOptionPane.showMessageDialog(this,
								"This hex cannot be upgraded now");
					}
				}
			}
		}

		// FIXME: Kludgy, but it forces the upgrades panel to be drawn
		// correctly.
		/*
		 * if (upgradesPanel != null) { upgradesPanel.setVisible(false);
		 * upgradesPanel.setVisible(true); showUpgrades(); }
		 */
	}

	private void selectHex(GUIHex clickedHex)
	{
		if (selectedHex != null && clickedHex != selectedHex)
		{
			selectedHex.setSelected(false);
			repaint(selectedHex.getBounds());
			selectedHex = null;
		}

		if (clickedHex != null)
		{
			clickedHex.setSelected(true);
			selectedHex = clickedHex;
			repaint(selectedHex.getBounds());
		}
	}

	public void mouseEntered(MouseEvent arg0)
	{
	}

	public void mouseExited(MouseEvent arg0)
	{
	}

	public void mousePressed(MouseEvent arg0)
	{
	}

	public void mouseReleased(MouseEvent arg0)
	{
	}

	public GUIHex getSelectedHex()
	{
		return selectedHex;
	}

	public boolean isHexSelected()
	{
		return selectedHex != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent arg0)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent arg0)
	{
		Point point = arg0.getPoint();
		GUIHex hex = getHexContainingPoint(point);
		setToolTipText(hex != null ? hex.getToolTip() : "");
	}

	/*
	 * public void setUpgradesPanel(UpgradesPanel upgradesPanel) {
	 * this.upgradesPanel = upgradesPanel; }
	 */

	public void setSpecials(ArrayList specials)
	{
		extraTileLays.clear();
		unconnectedTileLays.clear();
		if (specials != null)
		{
			Iterator it = specials.iterator();
			SpecialTileLay stl;
			while (it.hasNext())
			{
				stl = (SpecialTileLay) it.next();
				if (stl.isExercised())
					continue;
				unconnectedTileLays.add(stl);
				if (stl.isExtra())
					extraTileLays.add(stl);

				// System.out.println("Special tile lay allowed on hex "
				// + stl.getLocation().getName() + ", extra="
				// + stl.isExtra());
			}
		}
	}
	
	/*
	 * public void showUpgrades() { if (upgradesPanel == null) return;
	 * 
	 * if (selectedHex == null || baseTokenLayingEnabled) {
	 * upgradesPanel.setUpgrades(null); } else { ArrayList upgrades =
	 * (ArrayList) selectedHex.getCurrentTile()
	 * .getValidUpgrades(selectedHex.getHexModel(),
	 * GameManager.getCurrentPhase()); upgradesPanel.setUpgrades(upgrades); }
	 * 
	 * invalidate(); upgradesPanel.showUpgrades(); }
	 * 
	 * public void setWindow(ORWindow window) { this.window = window; }
	 */

}
