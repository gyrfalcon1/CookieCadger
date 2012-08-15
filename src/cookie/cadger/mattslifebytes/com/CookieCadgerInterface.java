/*
 * Copyright (c) 2012, Matthew Sullivan <MattsLifeBytes.com / @MattsLifeBytes>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution. 
 * 3. By using this software, you agree to provide the Software Creator (Matthew
 *    Sullivan) exactly one drink of his choice under $10 USD in value if he
 *    requests it of you.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * First time in Java (+ Eclipse)... sorry for the mess!  M.S. 
 */

package cookie.cadger.mattslifebytes.com;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JOptionPane;
import javax.swing.JList;
import javax.swing.JScrollPane;
import java.awt.Font;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.browsermob.proxy.ProxyServer;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import cookie.cadger.mattslifebytes.com.SortedListModel.SortOrder;

import javax.swing.JComboBox;
import javax.swing.JButton;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultComboBoxModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.swing.JTextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JCheckBox;

public class CookieCadgerInterface extends JFrame
{
	private static final long serialVersionUID = 8342026239392268208L;

	private JPanel contentPane, requestsPanel, accountsPanel;
	private EnhancedListModel macListModel, domainListModel, requestListModel;
	private DefaultComboBoxModel interfaceListModel;
	private ZebraJList macList, domainList, requestList;
	private JTextArea txtConsole;
	private JScrollPane consoleScrollPane;
	private String pathToTshark;
	private ArrayList<Boolean> bCapturing = new ArrayList<Boolean>();
	private ArrayList<String> deviceName = new ArrayList<String>();
	private ArrayList<String> deviceDescription = new ArrayList<String>();
	private ArrayList<Process> deviceCaptureProcess = new ArrayList<Process>();
	private HashMap componentMap; // Cheers to Jesse Strickland (stackoverflow.com/questions/4958600/get-a-swing-component-by-name)
	private JPopupMenu macPopup, requestPopup;
	
	private WebDriver driver = null;
	private Sqlite3DB dbInstance = null;
	private RequestInterceptor requestIntercept;

	private void StartCapture(int ethDevNumber, String pcapFile) throws IOException
	{
		ProcessBuilder pb = null;
		Process proc = null;
		ProcessWatcher pw = null;
		InputStream is = null;
		
		Date capStart = new Date();
		ArrayList<String> nonTabbedOutput = new ArrayList<String>();
		
		if(pcapFile.isEmpty())
		{			
			Console("Opening '" + deviceName.get(ethDevNumber) + "' for traffic capture.", true);
			pb = new ProcessBuilder(new String[] { pathToTshark, "-i", deviceName.get(ethDevNumber), "-f", "tcp dst port 80 or udp src port 5353 or udp src port 138", "-T", "fields", "-e", "eth.src", "-e", "wlan.sa", "-e", "ip.src", "-e", "tcp.srcport", "-e", "tcp.dstport", "-e", "udp.srcport", "-e", "udp.dstport", "-e", "browser.command", "-e", "browser.server", "-e", "dns.resp.name", "-e", "http.host", "-e", "http.request.uri", "-e", "http.user_agent", "-e", "http.referer", "-e", "http.cookie" } );
			pb.redirectErrorStream(true);
			deviceCaptureProcess.set(ethDevNumber, pb.start());
			pw = new ProcessWatcher(deviceCaptureProcess.get(ethDevNumber));
			is = deviceCaptureProcess.get(ethDevNumber).getInputStream();
		}
		else
		{
			Console("Opening '" + pcapFile + "' for traffic capture.", true);
			pb = new ProcessBuilder(new String[] { pathToTshark, "-r", pcapFile, "-T", "fields", "-e", "eth.src", "-e", "wlan.sa", "-e", "ip.src", "-e", "tcp.srcport", "-e", "tcp.dstport", "-e", "udp.srcport", "-e", "udp.dstport", "-e", "browser.command", "-e", "browser.server", "-e", "dns.resp.name", "-e", "http.host", "-e", "http.request.uri", "-e", "http.user_agent", "-e", "http.referer", "-e", "http.cookie" } );
			pb.redirectErrorStream(true);
			proc = pb.start();
			pw = new ProcessWatcher(proc);
			is = proc.getInputStream();
		}
		
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		String line = null;
		try
		{
			while ((line = br.readLine()) != null || pw.isFinished()) // Do while you have data to read, or if the process finishes start the loop to ensure all buffer has read, then exit
			{
				if(line == null) // Can happen while loading in and the proc finishes faster than readin
				{
					break;
				}

				if(line.contains("\t"))
				{
					String[] values = line.split("\t", -1); // -1 limit means don't ignore whitespace

					if(values.length > 4) // If only contains four or less values, likely continuation data, ignore.
					{					
						String macAddressWired = values[0];
						String macAddressWireless = values[1];
						
						String macAddress;
						if(!macAddressWired.isEmpty())
							macAddress = macAddressWired;
						else if(!macAddressWireless.isEmpty())
							macAddress = macAddressWireless;
						else // No MAC would hopefully never actually happen, but you never know
							macAddress = "Unknown";
						
						String ipAddress = values[2];
						//String tcpSource = values[3]; //Unused
						String tcpDestination = values[4];
						String udpSource = values[5];
						//String udpDestination = values[6]; //Unused
						String netbiosCommand = values[7];
						String netbiosName = values[8];
						String mdnsName = values[9];
						String requestHost = values[10];
						String requestURI = values[11];
						String userAgent = values[12];
						String refererURI = values[13];
						String cookieData = values[14];
						
						// Poor man's implementation of a packet filter for when pcaps are loaded
						if(!pcapFile.isEmpty() && (!tcpDestination.equals("80") && !udpSource.equals("5353") && !udpSource.equals("138")))
						{
							continue;
						}

						if(!requestURI.isEmpty())
						{
							ProcessRequest(macAddress, ipAddress, requestHost, requestURI, userAgent, refererURI, cookieData);
						}
						else if(!netbiosCommand.isEmpty() && netbiosCommand.equals("0x0f") && !netbiosName.isEmpty()) // 0x0f = host announcement broadcast
						{
							try
							{
								if(dbInstance.containsValue("clients", "mac", macAddress))
								{
									// We've seen this mac already, just set the hostname
									dbInstance.setStringValue("clients", "netbios_hostname", netbiosName, "mac", macAddress);
								}
								else // Client object doesn't exist for MAC? Create one.
								{
									dbInstance.createClient(macAddress, ipAddress);
									dbInstance.setStringValue("clients", "netbios_hostname", netbiosName, "mac", macAddress);
								}
								
								// If only show hosts with GET requests is unchecked, always show. If checked and total for this MAC > 0, show as well
								if(!macListModel.contains(macAddress) && !((JCheckBox)GetComponentByName("chckbxOnlyShowHosts")).isSelected() || ( ((JCheckBox)GetComponentByName("chckbxOnlyShowHosts")).isSelected() && !macListModel.contains(macAddress) && dbInstance.getDomainCount(macAddress) > 0 ) )
								{
									//macListModel.addElement(macAddress);
									macListModel.addElement(new EnhancedJListItem(-1, macAddress, null));
									macList.performHighlight(macAddress, Color.BLUE);
								}
								
								// And update the informational display
								//DisplayInfoForMac(macAddress);
								UpdateDescriptionForMac(macAddress);
							}
							catch (SQLException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						else if(!mdnsName.isEmpty())
						{
							if(mdnsName.contains(","))
							{
								String[] mdnsResponses = mdnsName.split(",");
								String mdnsNameStr = mdnsResponses[mdnsResponses.length - 1];
								
								if(!mdnsNameStr.contains(".arpa") && !mdnsNameStr.contains("_tcp") && !mdnsNameStr.contains("_udp") && !mdnsNameStr.contains("<Root>"))
								{									
									mdnsNameStr = mdnsNameStr.replace(".local", "");
									
									try
									{
										if(dbInstance.containsValue("clients", "mac", macAddress))
										{
											// We've seen this mac already, just set the hostname
											dbInstance.setStringValue("clients", "mdns_hostname", mdnsNameStr, "mac", macAddress);
										}
										else // Client object doesn't exist for MAC? Create one.
										{
											dbInstance.createClient(macAddress, ipAddress);
											dbInstance.setStringValue("clients", "mdns_hostname", mdnsNameStr, "mac", macAddress);
										}
										
										// If only show hosts with GET requests is unchecked, always show. If checked and total for this MAC > 0, show as well
										if(!macListModel.contains(macAddress) && !((JCheckBox)GetComponentByName("chckbxOnlyShowHosts")).isSelected() || ( ((JCheckBox)GetComponentByName("chckbxOnlyShowHosts")).isSelected() && !macListModel.contains(macAddress) && dbInstance.getDomainCount(macAddress) > 0 ) )
										{
											macListModel.addElement(new EnhancedJListItem(-1, macAddress, null));
											macList.performHighlight(macAddress, Color.BLUE);
										}
										
										// And update the informational display
										//DisplayInfoForMac(macAddress);
										UpdateDescriptionForMac(macAddress);
									}
									catch (SQLException e)
									{
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
						}
					}
				}
				else
				{
					nonTabbedOutput.add(line);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Console("EXCEPTION", true);
			
			if(!pcapFile.isEmpty() && proc != null)
			{
				proc.destroy();
			}
			else
			{
				if(deviceCaptureProcess.get(ethDevNumber) != null)
					deviceCaptureProcess.get(ethDevNumber).destroy();
				
				// TODO - REMOVE THIS KILLALL CAPTURE PROCESSES ON EXCEPTION
				for (Process p : deviceCaptureProcess)
				{
					if(p != null)
							p.destroy();
				}
			}
		}
		finally
		{
			if(br != null)
				br.close();
			if(isr != null)
				isr.close();
			if(is != null)
				is.close();
		}
		
		if(pcapFile.isEmpty())
			StopCapture(ethDevNumber); // Update UI and clean up
		
		Date capEnd = new Date();
		int runTimeInSeconds = (int) ((capEnd.getTime() - capStart.getTime()) / 1000);
		
		if(pcapFile.isEmpty() && runTimeInSeconds <= 20)
		{
			Console(deviceName.get(ethDevNumber) + ": " + "============================================================================", true);
			Console(deviceName.get(ethDevNumber) + ": " + "Start of diagnostic information for interface '" + deviceName.get(ethDevNumber) + "'", true);
			Console(deviceName.get(ethDevNumber) + ": " + "============================================================================", true);
			
			for (String output : nonTabbedOutput)
			{
				Console(deviceName.get(ethDevNumber) + ": " + output, true);				
			}
			
			Console(deviceName.get(ethDevNumber) + ": " + "============================================================================", true);
			Console(deviceName.get(ethDevNumber) + ": " + "Potential error detected! Capture stopped / died in " + Integer.toString(runTimeInSeconds) + " seconds.", true);
			Console(deviceName.get(ethDevNumber) + ": " + "All messages from the 'tshark' program have been printed above to assist you in solving any errors.", true);
			Console(deviceName.get(ethDevNumber) + ": " + "============================================================================", true);
		}
		else
		{
			if(pcapFile.isEmpty())
				Console("'" + deviceName.get(ethDevNumber) + "' has been closed and is finished with traffic capture. Capture duration: " + Integer.toString(runTimeInSeconds) + " seconds.", true);
			else
				Console("'" + pcapFile + "' has finished processing. Processing duration: " + Integer.toString(runTimeInSeconds) + " seconds.", true);
		}
	}
	
	private void StopCapture(int ethDevNumber)
	{
		interfaceListModel.removeElementAt(ethDevNumber);
		interfaceListModel.insertElementAt(deviceName.get(ethDevNumber) + " [" + deviceDescription.get(ethDevNumber) + "]", ethDevNumber);
		((JComboBox)GetComponentByName("interfaceListComboBox")).setSelectedIndex(ethDevNumber);

		if (deviceCaptureProcess.get(ethDevNumber) != null)
		{
			deviceCaptureProcess.get(ethDevNumber).destroy();
		}
		
		bCapturing.set(ethDevNumber, false);
		
		SetCaptureButtonText();
	}
	
	private void PrepCapture(int ethDevNumber)
	{
		interfaceListModel.removeElementAt(ethDevNumber);
		interfaceListModel.insertElementAt(deviceName.get(ethDevNumber) + " [" + deviceDescription.get(ethDevNumber) + "] (CURRENTLY CAPTURING)", ethDevNumber);
		((JComboBox)GetComponentByName("interfaceListComboBox")).setSelectedIndex(ethDevNumber);
		
		bCapturing.set(ethDevNumber, true);
		
		SetCaptureButtonText();
	}
	
	private void ResetSession()
	{
		try {
			dbInstance.initTables();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		requestListModel.clear();
		domainListModel.clear();
		macListModel.clear();
	}
	
	private void SetCaptureButtonText()
	{
		int selection = ((JComboBox)GetComponentByName("interfaceListComboBox")).getSelectedIndex();
		if(selection == -1)
		{
			((JButton)GetComponentByName("btnMonitorOnSelected")).setEnabled(false);
			((JButton)GetComponentByName("btnMonitorOnSelected")).setText("Select An Interface");
		}
		else
		{
			((JButton)GetComponentByName("btnMonitorOnSelected")).setEnabled(true);
			
			if(bCapturing.get(selection))
			{
				((JButton)GetComponentByName("btnMonitorOnSelected")).setText("Stop Capture on " + deviceName.get(selection));
			}
			else
			{
				((JButton)GetComponentByName("btnMonitorOnSelected")).setText("Start Capture on " + deviceName.get(selection));
			}
		}
	}
	
	private void ProcessRequest(String macAddress, String ipAddress, String requestHost, String requestURI, String userAgent, String refererURI, String cookieData)
	{
		int clientID = -1;
		int domainID = -1;
		int requestID = -1;
	
		try
		{
			if(dbInstance.containsValue("clients", "mac", macAddress))
			{
				// We've seen this mac already, just get the ClientID
				clientID = dbInstance.getIntegerValue("clients", "id", "mac", macAddress); // In 'clients' get id where mac == macAddrSource
				
				// We're going to have activity in a previously identified host, highlight
				macList.performHighlight(macAddress, Color.BLUE);
			}
			else // Client object doesn't exist for MAC? Create one.
			{
				clientID = dbInstance.createClient(macAddress, ipAddress);
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try
		{
			if(dbInstance.containsValue("domains", "name", requestHost))
			{
				// We've seen this domain already, just get the DomainID
				domainID = dbInstance.getIntegerValue("domains", "id", "name", requestHost);
			}
			else // Domain object doesn't exist for this name? Create one.
			{
				// Create new domain
				domainID = dbInstance.createDomain(requestHost);
				
				// And display it, if appropriate to do so
				if(!macList.isSelectionEmpty() && ((EnhancedJListItem)macList.getSelectedValue()).toString().equals(macAddress))
				{
					domainListModel.addElement(new EnhancedJListItem(-1, requestHost, null));
				}
			}
			
			// And highlight it for activity
			domainList.performHighlight(requestHost, Color.BLUE);
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		try
		{
			requestID = dbInstance.createRequest(requestURI, userAgent, refererURI, cookieData, domainID, clientID);
			
			// Update the requests list if necessary
			if(!macList.isSelectionEmpty() && ((EnhancedJListItem)macList.getSelectedValue()).toString().equals(macAddress) && !domainList.isSelectionEmpty() && (((EnhancedJListItem)domainList.getSelectedValue()).toString().equals(requestHost) || (((EnhancedJListItem)domainList.getSelectedValue()).toString().equals("[ All Domains ]"))))
			{
				Date now = new Date();	
				String dateString = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(now);

				//String idFormatted = String.format("%4d", requestID);
				//requestListModel.addElement("#" + idFormatted + " @ " + dateString + ": " + requestURI);
				requestListModel.addElement(new EnhancedJListItem(requestID, dateString + ": " + requestURI, null));
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			if(!macListModel.contains(macAddress) && !((JCheckBox)GetComponentByName("chckbxOnlyShowHosts")).isSelected() || ( ((JCheckBox)GetComponentByName("chckbxOnlyShowHosts")).isSelected() && !macListModel.contains(macAddress) && dbInstance.getDomainCount(macAddress) > 0 ) )
			{
				macListModel.addElement(new EnhancedJListItem(-1, macAddress, null));
				macList.performHighlight(macAddress, Color.BLUE);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		UpdateDescriptionForMac(macAddress);
		
		// Check if extended information can be gleaned for domains
		if(requestHost.contains("facebook.com") && cookieData.contains("c_user="))
		{
			int c_userPosition = cookieData.indexOf("c_user=");
			String fbUserID = cookieData.substring(c_userPosition + 7);
			if(fbUserID.contains(";"))
				fbUserID = fbUserID.substring(0, fbUserID.indexOf(";"));
			
			try {
				JSONObject fbJSON = new JSONObject(readUrl("http://graph.facebook.com/" + fbUserID));
				String fbName = (String) fbJSON.get("name");
				String fbUsername = (String) fbJSON.get("username");
				
				Console(fbName + "|" + fbUsername, true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(requestHost.contains("twitter.com") && cookieData.contains("twid="))
		{
			int twidPosition = cookieData.indexOf("twid=");
			String twitterUserID = cookieData.substring(twidPosition + 5);

			if(twitterUserID.contains(";"))
				twitterUserID = twitterUserID.substring(0, twitterUserID.indexOf(";"));
			
			try {
				twitterUserID = URLDecoder.decode(twitterUserID, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			twitterUserID = twitterUserID.substring(2, twitterUserID.indexOf("|"));
			
			try {
				JSONObject twitterJSON = new JSONObject(readUrl("https://api.twitter.com/users/lookup.json?user_id=" + twitterUserID));
				String twitterName = (String) twitterJSON.get("screen_name");
				Console(twitterName, true);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void ChangeMacList()
	{
		boolean bPreviousSelection = false;
		String previousSelection = null;
		
		if(!macList.isSelectionEmpty())
		{
			bPreviousSelection = true;
			previousSelection = ((EnhancedJListItem)macList.getSelectedValue()).toString();
		}
		
		macListModel.clear();
		
		try
		{
			boolean bOnlyHostsWithData = ((JCheckBox)GetComponentByName("chckbxOnlyShowHosts")).isSelected();

			for (String s : dbInstance.getMacs())
			{
				// Are we only supposed to show hosts with recorded GET requests?
				if(bOnlyHostsWithData)
				{
					if(dbInstance.getDomainCount(s) == 0) // Domains NOT found for this host
					{
						continue;
					}
				}
				
				macListModel.addElement(new EnhancedJListItem(-1, s, null));
				UpdateDescriptionForMac(s);
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(bPreviousSelection)
		{
			// If the newly generated list still contains the previously selected value, show it
			if (macListModel.contains(previousSelection))
			{
				int index = macListModel.indexOf(previousSelection);
				macList.setSelectedValue(macListModel.getElementAt(index), true);
			}
		}
	}
	
	private void ChangeDomainList(String macAddress)
	{
		boolean bPreviousSelection = false;
		String previousSelection = null;
		
		if(!domainList.isSelectionEmpty())
		{
			bPreviousSelection = true;
			previousSelection = ((EnhancedJListItem)domainList.getSelectedValue()).toString();
		}
		
		domainListModel.clear();
		requestListModel.clear();
		
		domainListModel.addElement(new EnhancedJListItem(-1, "[ All Domains ]", null));
		try {
			for (String s : dbInstance.getDomains(macAddress))
			{
			      domainListModel.addElement(new EnhancedJListItem(-1, s, null));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(bPreviousSelection)
		{
			// If the newly generated list still contains the previously selected value, show it
			if (domainListModel.contains(previousSelection))
			{
				int index = domainListModel.indexOf(previousSelection);
				domainList.setSelectedValue(domainListModel.getElementAt(index), true);
			}
		}
	}
	
	private void ChangeRequestList(String macAddress, String uriHost)
	{	
		boolean bShowAllDomains = false;
		requestListModel.clear();
		
		if(uriHost.equals("[ All Domains ]"))
		{
			bShowAllDomains = true;
			uriHost = "%";
		}
		
		try {
			ArrayList<ArrayList> requests = dbInstance.getRequests(macAddress, uriHost);
			
			ArrayList<String> ids = requests.get(0);
			ArrayList<String> timerecordeds = requests.get(1);
			ArrayList<String> uris = requests.get(2);
			
			for (int i = 0; i < ids.size(); i++)
			{
				long timeStamp = new Long(timerecordeds.get(i));
				Date then = new Date((long)timeStamp * 1000);
				Date now = new Date();
				
				SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
				boolean sameDay = fmt.format(then).equals(fmt.format(now));
				
				String dateString;
				if(sameDay)
				{
					dateString = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(then);
				}
				else
				{
					dateString = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(then);
				}

				int requestID = new Integer(ids.get(i));
				//String idFormatted = String.format("%4d", requestID);
				String uri = uris.get(i);
				
				// If showing all domains in the request list, make sure the full request URL gets displayed
				if(bShowAllDomains)
				{
					int domainID = dbInstance.getIntegerValue("requests", "domain_id", "id", Integer.toString(requestID));
					String domain = dbInstance.getStringValue("domains", "name", "id", Integer.toString(domainID));
					uri = domain + uri;
				}
				
				//requestListModel.addElement("#" + idFormatted + " @ " + dateString + ": " + uri);
				requestListModel.addElement(new EnhancedJListItem(requestID, dateString + ": " + uri, null));
				
				UpdateDescriptionForRequest(requestID);
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void UpdateDescriptionForMac(String macAddress)
	{
		if(macListModel.contains(macAddress))
		{
			int location = macListModel.indexOf(macAddress);
			((EnhancedJListItem)macListModel.getElementAt(location)).setDescription(GenerateDescriptionForMac(macAddress, true));
		}
	}
	
	private String GenerateDescriptionForMac(String macAddress, boolean bUseHTML)
	{
		String htmlOpen = "";
		String boldOpen = "";
		String fontOpen = "";
		String fontClose = "";
		String boldClose = "";
		String htmlClose = "";
		String newLine = "\r\n";
		
		if(bUseHTML)
		{
			htmlOpen = "<html>";
			boldOpen = "<b>";
			fontOpen = "<font size=4>";
			fontClose = "</font>";
			boldClose = "</b>";
			htmlClose = "</html>";
			newLine = "<br>";
		}
		
		try {
			String notesTxt = htmlOpen + fontOpen + boldOpen + "IP Address: " + boldClose + dbInstance.getStringValue("clients", "ip", "mac", macAddress);
			String netbiosHost = dbInstance.getStringValue("clients", "netbios_hostname", "mac", macAddress);
			String mdnsHost = dbInstance.getStringValue("clients", "mdns_hostname", "mac", macAddress);
			
			if(!netbiosHost.isEmpty())
			{
				notesTxt = notesTxt + newLine + boldOpen + "Host Name (NetBIOS): " + boldClose + netbiosHost;
			}
			
			if(!mdnsHost.isEmpty())
			{
				notesTxt = notesTxt + newLine + boldOpen + "Host Name (mDNS/Bonjour): " + boldClose + mdnsHost;
			}

			notesTxt = notesTxt + fontClose + htmlClose;
			
			return notesTxt;

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	private void UpdateDescriptionForRequest(int request)
	{
		if(requestListModel.contains(request))
		{
			int location = requestListModel.indexOf(request);
			((EnhancedJListItem)requestListModel.getElementAt(location)).setDescription(GenerateDescriptionForRequest(request, true, true));
		}
	}
	
	private String GenerateDescriptionForRequest(int request, boolean bUseHTML, boolean bTruncate)
	{
		String htmlOpen = "";
		String boldOpen = "";
		String fontOpen = "";
		String fontClose = "";
		String boldClose = "";
		String htmlClose = "";
		String newLine = "\r\n";
		
		if(bUseHTML)
		{
			htmlOpen = "<html>";
			boldOpen = "<b>";
			fontOpen = "<font size=4>";
			fontClose = "</font>";
			boldClose = "</b>";
			htmlClose = "</html>";
			newLine = "<br>";
		}
		
		try {
			String timeRecorded = dbInstance.getStringValue("requests", "timerecorded", "id", Integer.toString(request));
			
			long timeStamp = new Long(timeRecorded);
			Date then = new Date((long)timeStamp * 1000);		
			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
			String dateString = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(then);
			
			String uri = dbInstance.getStringValue("requests", "uri", "id", Integer.toString(request));
			if(bTruncate && uri.length() > 90)
				uri = uri.substring(0, 86) + boldOpen + " ..." + boldClose;
			 
			String notesTxt = htmlOpen + fontOpen + boldOpen + "Date: " + boldClose + dateString;
			notesTxt = notesTxt + newLine + boldOpen + "URI: " + boldClose + uri;
			String userAgent = dbInstance.getStringValue("requests", "useragent", "id", Integer.toString(request));
			String referer = dbInstance.getStringValue("requests", "referer", "id", Integer.toString(request));
			String cookies = dbInstance.getStringValue("requests", "cookies", "id", Integer.toString(request));
			
			if(!userAgent.isEmpty())
			{
				if(bTruncate && userAgent.length() > 90)
					userAgent = userAgent.substring(0, 86) + boldOpen + " ..." + boldClose;
				
				notesTxt = notesTxt + newLine + boldOpen + "User Agent: " + boldClose + userAgent;
			}
			
			if(!referer.isEmpty())
			{
				if(bTruncate && referer.length() > 90)
					referer = referer.substring(0, 86) + boldOpen + " ..." + boldClose;
				
				notesTxt = notesTxt + newLine + boldOpen + "Referer: " + boldClose + userAgent;
			}
			
			if(!cookies.isEmpty())
			{
				if(bTruncate && cookies.length() > 90)
					cookies = cookies.substring(0, 86) + boldOpen + " ..." + boldClose;
				
				notesTxt = notesTxt + newLine + boldOpen + "Cookies: " + boldClose + cookies;
			}
			
			return notesTxt;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	// Now we're at the good part.
	private void LoadCookiesIntoBrowser(String domain, String uri, String useragent, String referer, String cookies, boolean bReplay)
	{				
		if(driver == null)
		{
	        ProxyServer server = new ProxyServer(7878);
	        Proxy proxy = null;
	        
	        requestIntercept = new RequestInterceptor();
	        
	        try {
				server.start();
				server.addRequestInterceptor(requestIntercept);
				server.setDownstreamKbps(1024);
				proxy = server.seleniumProxy().setSslProxy(null); // Set HTTP proxy, but unset SSL entirely
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
			// configure it as a desired capability
			DesiredCapabilities capabilities = new DesiredCapabilities();
			capabilities.setCapability(CapabilityType.PROXY, proxy);
			
			driver = new FirefoxDriver(capabilities);
		}
		else
		{
			// Everything is already set to go, just clear the proxy settings
			requestIntercept.clear();
		}
		
        if(!cookies.isEmpty())
        {
        	requestIntercept.setCookies(cookies);
        }
        
        if(useragent.isEmpty())
        {
        	// None specifically specified, so load from Firefox via the WebDriver
        	// (Without this BrowserMob modifies it and adds a unique tag)
        	useragent = (String) ((JavascriptExecutor) driver).executeScript("return navigator.userAgent;");
        }
        requestIntercept.setUserAgent(useragent);
        
        if(!referer.isEmpty())
        {
        	requestIntercept.setReferer(referer);
        }

        driver.get("http://" + domain + uri);
	}
	
	private void PrepareToCloseApplication()
	{
		// Get the WebDriver fully unloaded
		try
		{
			driver.close();
		}
		catch (Exception ex)
		{}
		finally
		{
			driver = null;
		}
		
		try
		{
			for (Process p : deviceCaptureProcess)
			{
				if(p != null)
					p.destroy();
			}
			
			if(dbInstance != null)
				dbInstance.closeDatabase();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public CookieCadgerInterface(String pathToTshark) throws Exception
	{
		this.pathToTshark = pathToTshark;
		
		setResizable(false);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e)
			{
				super.windowClosing(e);
				PrepareToCloseApplication();
			}
		});	    
		dbInstance = new Sqlite3DB();
		
		setTitle("Cookie Cadger");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 950, 680);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmStartNewSession = new JMenuItem("Start New Session");
		mnFile.add(mntmStartNewSession);
		
		JSeparator separator = new JSeparator();
		mnFile.add(separator);
		
		JMenuItem mntmOpenACapture = new JMenuItem("Open a Capture File (*.pcap)");
		mnFile.add(mntmOpenACapture);
		
		mnFile.add(new JSeparator());
		
		JMenuItem mntmSaveSession = new JMenuItem("Save Session");
		mnFile.add(mntmSaveSession);
		
		JMenuItem mntmLoadSession = new JMenuItem("Load Session");
		mnFile.add(mntmLoadSession);
		
		mnFile.add(new JSeparator());
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mnFile.add(mntmExit);
		
		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		
		JMenuItem mntmCopySelectedRequest = new JMenuItem("Copy Selected Request to Clipboard");
		mnEdit.add(mntmCopySelectedRequest);
		
		JMenuItem mntmCopyAllRequests = new JMenuItem("Copy All Requests to Clipboard");
		mnEdit.add(mntmCopyAllRequests);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		JMenuItem mntmAbout = new JMenuItem("About Cookie Cadger");
		mnHelp.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBounds(28, 48, 894, 396);

		requestsPanel = new JPanel();
		requestsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		accountsPanel = new JPanel();
		requestsPanel.setLayout(null);
		accountsPanel.setLayout(null);
		
		tabbedPane.addTab("Requests", null, requestsPanel, null);
		tabbedPane.addTab("Recognized Logins", null, accountsPanel, null);
		
		contentPane.add(tabbedPane);
		
		macListModel = new EnhancedListModel();
		domainListModel = new EnhancedListModel();
		requestListModel = new EnhancedListModel();
		interfaceListModel = new DefaultComboBoxModel();
		
		txtConsole = new JTextArea();
		txtConsole.setBackground(UIManager.getColor("Panel.background"));
		txtConsole.setFont(new Font("Verdana", Font.BOLD, 12));
		txtConsole.setEditable(false);
		
		consoleScrollPane = new JScrollPane();
		consoleScrollPane.setBounds(28, 455, 895, 150);
		contentPane.add(consoleScrollPane);
		consoleScrollPane.setViewportView(txtConsole);
		
		JButton btnMonitorOnSelected = new JButton("Select An Interface");
		btnMonitorOnSelected.setEnabled(false);
		btnMonitorOnSelected.setName("btnMonitorOnSelected");
		btnMonitorOnSelected.setBounds(681, 14, 240, 25);
		contentPane.add(btnMonitorOnSelected);
		
		JComboBox interfaceListComboBox = new JComboBox();
		interfaceListComboBox.setName("interfaceListComboBox");
		interfaceListComboBox.setBounds(28, 14, 626, 24);
		contentPane.add(interfaceListComboBox);
		
		interfaceListComboBox.setModel(interfaceListModel);
		
		JScrollPane macListScrollPanel = new JScrollPane();
		macListScrollPanel.setBounds(2, 2, 162, 328);
		requestsPanel.add(macListScrollPanel);
		
		macList = new ZebraJList();
		
		macList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		macList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if(!e.getValueIsAdjusting() && !macList.isSelectionEmpty())
				{
					JList list = (JList)e.getSource();
					String item = ((EnhancedJListItem)list.getSelectedValue()).toString();
					ChangeDomainList(item);
				}
			}
		});

		macListScrollPanel.setViewportView(macList);
		SortedListModel macListModelSorted = new SortedListModel(macListModel);
		macListModelSorted.setSortOrder(SortOrder.ASCENDING);
		macList.setModel(macListModelSorted);
		
		JCheckBox chckbxOnlyShowHosts = new JCheckBox("<html>Only show hosts<br>with HTTP traffic");
		chckbxOnlyShowHosts.setName("chckbxOnlyShowHosts");
		chckbxOnlyShowHosts.setSelected(true);
		chckbxOnlyShowHosts.setBounds(2, 331, 162, 38);
		requestsPanel.add(chckbxOnlyShowHosts);
		
		JScrollPane domainListScrollPane = new JScrollPane();
		domainListScrollPane.setBounds(170, 2, 200, 366);
		requestsPanel.add(domainListScrollPane);
		
		domainList = new ZebraJList();
		
		domainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		domainList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if(!e.getValueIsAdjusting() && !macList.isSelectionEmpty() && !domainList.isSelectionEmpty())
				{
					JList list = (JList)e.getSource();
					String item = ((EnhancedJListItem)list.getSelectedValue()).toString();
					String macAddress = ((EnhancedJListItem)macList.getSelectedValue()).toString();
					ChangeRequestList(macAddress, item);
				}
			}
		});
		
		domainListScrollPane.setViewportView(domainList);
		SortedListModel domainListModelSorted = new SortedListModel(domainListModel);
		domainListModelSorted.setSortOrder(SortOrder.ASCENDING);
		domainList.setModel(domainListModelSorted);
		
		JScrollPane requestListScrollPanel = new JScrollPane();
		requestListScrollPanel.setBounds(376, 2, 513, 336);
		requestsPanel.add(requestListScrollPanel);

		requestList = new ZebraJList();
		
		Font fontMonospace = new Font( "Monospaced", Font.BOLD, 12 ); 
		requestList.setFont(fontMonospace);
		requestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		requestList.setModel(requestListModel);
		requestListScrollPanel.setViewportView(requestList);
		
		macPopup = new JPopupMenu();
		JMenuItem macMenuItem = new JMenuItem("Copy Host Information");
		macMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!macList.isSelectionEmpty())
				{
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Clipboard clipboard = toolkit.getSystemClipboard();
					String macAddress = ((EnhancedJListItem)macList.getSelectedValue()).toString();
					StringSelection strSel = new StringSelection(GenerateDescriptionForMac(macAddress, false));
					clipboard.setContents(strSel, null);
				}
			}
		});
	    macPopup.add(macMenuItem);
	    
	    macList.addMouseListener(new MouseAdapter()
	    {
	    	public void mouseClicked(MouseEvent me)
	    	{
	            if (SwingUtilities.isRightMouseButton(me))
	            {
	            	// Disable ToolTip for 3 seconds
	            	SwingWorker worker = new SwingWorker() {            
	                	@Override            
	                    public Object doInBackground() {
	                        try {
	                        	ToolTipManager.sharedInstance().setEnabled(false);
	                            Thread.sleep(3000);
	                        } catch (InterruptedException e) { /*Who cares*/ }
	                        return null;
	                    }
	                    @Override
	                    public void done()
	                    {
	                    	ToolTipManager.sharedInstance().setEnabled(true);
	                    }
	                };
	                worker.execute();
	            	
	                Point mousePosition = me.getPoint();
	                int index = macList.locationToIndex(me.getPoint());
        			Rectangle cellRect = macList.getCellBounds(index, index);
        			
        			// If point inside rectangle
        			if(mousePosition.x >= cellRect.getMinX() && mousePosition.x < cellRect.getMaxX() && mousePosition.y >= cellRect.getMinY() && mousePosition.y < cellRect.getMaxY())
        			{
    	            	// If right-clicked item is not currently selected, do that
    	            	if( index != macList.getSelectedIndex())
    	            		macList.setSelectedIndex(index);
    	            	
    	            	macPopup.show(macList, me.getX(), me.getY());
        			}
	            }
	    	}
	    });
	    
		requestPopup = new JPopupMenu();
		JMenuItem requestMenuItem = new JMenuItem("Copy Request Information");
		requestMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!requestList.isSelectionEmpty())
				{
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Clipboard clipboard = toolkit.getSystemClipboard();
					int request = ((EnhancedJListItem)requestList.getSelectedValue()).getID();
					StringSelection strSel = new StringSelection(GenerateDescriptionForRequest(request, false, false));				
					clipboard.setContents(strSel, null);
				}
			}
		});
	    requestPopup.add(requestMenuItem);
	    
	    requestList.addMouseListener(new MouseAdapter()
	    {
	    	public void mouseClicked(MouseEvent me)
	    	{
	            if (SwingUtilities.isRightMouseButton(me))
	            {
	            	// Disable ToolTip for 3 seconds
	            	SwingWorker worker = new SwingWorker() {            
	                	@Override            
	                    public Object doInBackground() {
	                        try {
	                        	ToolTipManager.sharedInstance().setEnabled(false);
	                            Thread.sleep(3000);
	                        } catch (InterruptedException e) { /*Who cares*/ }
	                        return null;
	                    }
	                    @Override
	                    public void done()
	                    {
	                    	ToolTipManager.sharedInstance().setEnabled(true);
	                    }
	                };
	                worker.execute();
	                
	                Point mousePosition = me.getPoint();
	                int index = requestList.locationToIndex(me.getPoint());
        			Rectangle cellRect = requestList.getCellBounds(index, index);
        			
        			// If point inside rectangle
        			if(mousePosition.x >= cellRect.getMinX() && mousePosition.x < cellRect.getMaxX() && mousePosition.y >= cellRect.getMinY() && mousePosition.y < cellRect.getMaxY())
        			{
    	            	// If right-clicked item is not currently selected, do that
    	            	if( index != requestList.getSelectedIndex())
    	            		requestList.setSelectedIndex(index);
    	            	
    	            	requestPopup.show(requestList, me.getX(), me.getY());
        			}
	            }
	    	}
	    });
	    
		JButton btnLoadDomainCookies = new JButton("Load Domain Cookies");
		btnLoadDomainCookies.setBounds(376, 342, 254, 25);
		requestsPanel.add(btnLoadDomainCookies);
		
		JButton btnReplayRequest = new JButton("Replay This Request");
		btnReplayRequest.setBounds(634, 342, 254, 25);
		requestsPanel.add(btnReplayRequest);
		
		/*
		 * Create and associate the ActionListeners for all objects
		 */
		
		mntmStartNewSession.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ResetSession();
			}
		});

		mntmOpenACapture.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Thread captureThread = new Thread(new Runnable()
				{
					public void run()
					{
						JFileChooser fc = new JFileChooser();
						FileFilter pcapFilter = new FileNameExtensionFilter("*.pcap | *.cap | *.pcapdump", "pcap", "cap", "pcapdump");
						fc.addChoosableFileFilter(pcapFilter);
						fc.setFileFilter(pcapFilter);

						int returnVal = fc.showOpenDialog(contentPane);

				        if (returnVal == JFileChooser.APPROVE_OPTION)
				        {
				            File file = fc.getSelectedFile();
				            JOptionPane.showMessageDialog(null, "This process could take some time, please be patient.");
						            
							try {
								StartCapture(-1, file.getAbsolutePath());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
				        }
					}
				});
					
				captureThread.start();
			}
		});


		mntmSaveSession.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dbInstance.saveDatabase();
			}
		});


		mntmLoadSession.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ResetSession();
				dbInstance.openDatabase();
				ChangeMacList();
			}
		});


		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				PrepareToCloseApplication();
				dispose();
			}
		});


		mntmCopySelectedRequest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(requestList.isSelectionEmpty())
				{
					JOptionPane.showMessageDialog(null, "You must first select a request.");
				}
				else
				{
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Clipboard clipboard = toolkit.getSystemClipboard();				
					int request = ((EnhancedJListItem)requestList.getSelectedValue()).getID();
					StringSelection strSel = new StringSelection(GenerateDescriptionForRequest(request, false, false));
					clipboard.setContents(strSel, null);
				}
			}
		});


		mntmCopyAllRequests.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String allRequests = "";
				
				for (int i = 0; i < requestListModel.getSize(); i++)
				{
					int request = ((EnhancedJListItem)requestListModel.getElementAt(i)).getID();
					allRequests = allRequests + GenerateDescriptionForRequest(request, false, false);
					
					if( i < requestListModel.getSize() - 1)
						allRequests = allRequests + "\r\n\r\n---\r\n\r\n";
				}
				
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Clipboard clipboard = toolkit.getSystemClipboard();
				StringSelection strSel = new StringSelection(allRequests);
				clipboard.setContents(strSel, null);	
			}
		});


		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				JOptionPane.showMessageDialog(null, "Cookie Cadger\n\n" +
						"Copyright (c) 2012, Matthew Sullivan <MattsLifeBytes.com / @MattsLifeBytes>\n" +
						"All rights reserved.\n" +
						"\n" +
						"Redistribution and use in source and binary forms, with or without\n" +
						"modification, are permitted provided that the following conditions are met: \n" +
						"\n" +
						"1. Redistributions of source code must retain the above copyright notice, this\n" +
						"   list of conditions and the following disclaimer. \n" +
						"2. Redistributions in binary form must reproduce the above copyright notice,\n" +
						"   this list of conditions and the following disclaimer in the documentation\n" +
						"   and/or other materials provided with the distribution. \n" +
						"3. By using this software, you agree to provide the Software Creator (Matthew\n" +
						"   Sullivan) exactly one drink of his choice under $10 USD in value if he\n" +
						"   requests it of you.\n" +
						"\n" +
						"THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND\n" +
						"ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\n" +
						"WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n" +
						"DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR\n" +
						"ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES\n" +
						"(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;\n" +
						"LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND\n" +
						"ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" +
						"(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\n" +
						"SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
						);
			}
		});


		btnLoadDomainCookies.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0)
			{
				if(((EnhancedJListItem)domainList.getSelectedValue()).toString().equals("[ All Domains ]"))
				{
					JOptionPane.showMessageDialog(null, "This option is not available when \"[ All Domains ]\" is selected.");
				}
				else if(!domainList.isSelectionEmpty())
				{
					Thread loadCookiesThread = new Thread(new Runnable()
					{
						public void run()
						{
							try {
								int domainID = dbInstance.getIntegerValue("domains", "id", "name", ((EnhancedJListItem)domainList.getSelectedValue()).toString());
								int requestID = dbInstance.getNewestRequestID(domainID);

								String domain = ((EnhancedJListItem)domainList.getSelectedValue()).toString();
								String uri = "";
								String useragent = "";
								String referer = "";
								String cookies = dbInstance.getStringValue("requests", "cookies", "id", Integer.toString(requestID));
								
								LoadCookiesIntoBrowser(domain, uri, useragent, referer, cookies, false);
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
		
					loadCookiesThread.start();
				}
			}
		});


		btnReplayRequest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0)
			{
				if(!requestList.isSelectionEmpty())
				{					
					Thread loadCookiesThread = new Thread(new Runnable()
					{
						public void run()
						{
							//String request = (String)requestList.getSelectedValue();
							//int requestID = Integer.parseInt(request.substring(1, request.indexOf("@")).trim());
							EnhancedJListItem listItem = (EnhancedJListItem)requestList.getSelectedValue();
							int requestID = listItem.getID();
							
							try {
								int domainID = dbInstance.getIntegerValue("requests", "domain_id", "id", Integer.toString(requestID));
								String domain = dbInstance.getStringValue("domains", "name", "id", Integer.toString(domainID));
								String uri = dbInstance.getStringValue("requests", "uri", "id", Integer.toString(requestID));
								String useragent = dbInstance.getStringValue("requests", "useragent", "id", Integer.toString(requestID));
								String referer = dbInstance.getStringValue("requests", "referer", "id", Integer.toString(requestID));
								String cookies = dbInstance.getStringValue("requests", "cookies", "id", Integer.toString(requestID));
								
								LoadCookiesIntoBrowser(domain, uri, useragent, referer, cookies, true);
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
		
					loadCookiesThread.start();
				}
			}
		});


		interfaceListComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				SetCaptureButtonText();
			}
		});


		btnMonitorOnSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				int selectedInterface = ((JComboBox)GetComponentByName("interfaceListComboBox")).getSelectedIndex();
				if(selectedInterface == -1)
					return;
					
				boolean bInterfaceIsCapturing = bCapturing.get(selectedInterface); // Make a copy because this value will be changing a lot...
							
				if(bInterfaceIsCapturing)
				{
					StopCapture(selectedInterface);
				}
				else
				{
					PrepCapture(selectedInterface);
					
					Thread captureThread = new Thread(new Runnable()
					{
						public void run()
						{
							try {
								int selectedInterface = ((JComboBox)GetComponentByName("interfaceListComboBox")).getSelectedIndex();
								StartCapture(selectedInterface, "");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
					
					captureThread.start();
				}
			}
		});
		
		chckbxOnlyShowHosts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				domainListModel.clear();
				requestListModel.clear();
				
				// Status toggled, re-create the mac list
				ChangeMacList();
			}
		});
		
		// Associate all components with the HashMap
		componentMap = CreateComponentMap(contentPane);
				
		// Get capture devices
		InitializeDevices();
		
		// Name and license
		Console("\n\nCookie Cadger\nCreated by Matthew Sullivan - mattslifebytes.com\nThis software is freely distributed under the terms of the FreeBSD license.\n", true);
		
		// Populate the ComboBox
		int itemToSelect = -1;
		for (int i = 0; i < deviceName.size(); i++)
		{			
			boolean bIsMon = false;
			String interfaceText;
			
			if(deviceName.get(i).contains("mon0"))
			{
				bIsMon = true;
			}
			
			interfaceText = deviceName.get(i) + " [" + deviceDescription.get(i) + "]";
			interfaceListModel.addElement(interfaceText);
			
			if(bIsMon)
				itemToSelect = i;
		}
		
		interfaceListComboBox.setSelectedIndex(itemToSelect);
	}
	
	private void InitializeDevices()
	{
		File tshark;
		
		if(pathToTshark.isEmpty()) // no program arg specified
		{
			// Get tshark location by checking likely Linux, Windows, and Mac paths
			//							// Ubuntu/Debian	// Fedora/RedHat		// Windows 32-bit							//Windows 64-bit								//Mac OS X
			String[] pathCheckStrings = { "/usr/bin/tshark", "/usr/sbin/tshark", "C:\\Program Files\\Wireshark\\tshark.exe", "C:\\Program Files (x86)\\Wireshark\\tshark.exe", "/Applications/Wireshark.app/Contents/Resources/bin/tshark" };
			
			for(String path : pathCheckStrings)
			{
				if(new File(path).exists())
				{
					Console("tshark located at " + path, false);
					pathToTshark = path;
					break;
				}
			}
		}
		else // program arg specified, check that tshark exists there
		{
			if(new File(pathToTshark).exists())
			{
				Console("tshark specified at " + pathToTshark, false);
			}
			else
			{
				JOptionPane.showMessageDialog(null, "You specified a path to 'tshark' as an argument when starting this program, but the given path is invalid.");
				pathToTshark = ""; // Empty the user-specified value
			}
		}
		
		if(pathToTshark.isEmpty())
		{
			JOptionPane.showMessageDialog(null, "Error: couldn't find 'tshark' (part of the 'Wireshark' suite). This software cannot capture or analyze packets without it.\nYou can still load previously saved sessions for replaying in the browser, but be aware you might encounter errors.\n\nYou can manually specify the location to 'tshark' as a program argument.\n\nUsage:\njava -jar CookieCadger.jar <optional: full path to tshark>");
		}
		else
		{
			Console("Querying tshark for capture devices; tshark output follows:", false);

			String line = "";
			try {
				ProcessBuilder pb = new ProcessBuilder(new String[] { pathToTshark, "-D" } );
				pb.redirectErrorStream(true);
				Process proc = pb.start();
				InputStream is = proc.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				
				while ((line = br.readLine()) != null)
				{
					// Print every piece of output to the console
					Console(line, false);
					if(line.contains(". ")) // As in "1. eth1"
					{
						if(line.contains("(") && line.contains(")"))
						{
							// This line has a description
							deviceDescription.add(line.substring(line.indexOf(" (") + 2, line.indexOf(")")).trim());
							deviceName.add(line.substring(line.indexOf(". ") + 2, line.indexOf(" (")));
						}
						else
						{
							// This line has no description
							deviceDescription.add("no description");
							deviceName.add(line.substring(line.indexOf(" ") + 1));
						}
						bCapturing.add(false);
						deviceCaptureProcess.add(null);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Console("Capture device search completed with " + deviceName.size() + " devices found.", false);
		}
	}

	private void Console(String text, boolean bAutoScroll)
	{
		if(txtConsole.getText().isEmpty())
			txtConsole.setText(text);
		else
			txtConsole.append("\n" + text);
		
		if(bAutoScroll)
			txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
		
		System.out.println(text);
	}
	
	private HashMap CreateComponentMap(JPanel panel)
	{
		HashMap componentMap = new HashMap<String,Component>();
        Component[] components = panel.getComponents();
        for (int i=0; i < components.length; i++)
        {
        	if(components[i] instanceof JTabbedPane)
        	{
        		// Find the tabbed panel and iterate its components as well
        		for (Component cmp : ((JTabbedPane) components[i]).getComponents())
        		{
        			if(cmp instanceof JPanel)
        			{
        				for (Component x : ((JPanel) cmp).getComponents())
        				{
        		        	componentMap.put(x.getName(), x);
        				}
        			}
        		}
        	}
        	
       		componentMap.put(components[i].getName(), components[i]);
        }
        
        return componentMap;
	}

	private Component GetComponentByName(String name) {
        if (componentMap.containsKey(name)) {
                return (Component) componentMap.get(name);
        }
        else return null;
	}
	
	private static String readUrl(String urlString) throws Exception {
	    BufferedReader reader = null;
	    try {
	        URL url = new URL(urlString);
	        reader = new BufferedReader(new InputStreamReader(url.openStream()));
	        StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1)
	            buffer.append(chars, 0, read); 

	        return buffer.toString();
	    } finally {
	        if (reader != null)
	            reader.close();
	    }
	}
}