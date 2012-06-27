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

import java.awt.Component;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JList;
import javax.swing.JScrollPane;
import java.awt.Font;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jnetpcap.*;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import cookie.cadger.mattslifebytes.com.SortedListModel.SortOrder;

import javax.swing.JComboBox;
import javax.swing.JButton;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.DefaultComboBoxModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JTextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

public class CookieCadgerInterface extends JFrame
{
	private static final long serialVersionUID = 8342026239392268208L;
	
	private JPanel contentPane;
	private DefaultListModel macListModel, domainListModel, requestListModel;
	private DefaultComboBoxModel interfaceListModel;
	private ZebraJList macList, domainList, requestList;
	private JTextArea txtConsole, txtInformation;
	private JScrollPane consoleScrollPane, informationScrollPane;
	private int deviceCount = 0;
	private Boolean[] bCapturing;
	private String[] deviceName;
	private String[] deviceDescription;
	private Process[] deviceCaptureProcess;
	private HashMap componentMap; // Cheers to Jesse Strickland (stackoverflow.com/questions/4958600/get-a-swing-component-by-name)
	
	private static WebDriver driver = null;
	private static Sqlite3DB dbInstance = null;

	private void StartCapture(int ethDevNumber, String pcapFile) throws IOException
	{
		ProcessBuilder pb = null;
		Process proc = null;
		ProcessWatcher pw = null;
		InputStream is = null;
		
		PcapIf device = null;
		Date capStart = new Date();
		ArrayList<String> nonTabbedOutput = new ArrayList<String>();
		
		if(pcapFile.isEmpty())
		{
			List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with NICs
			StringBuilder errbuf = new StringBuilder();     // For any error msgs
			
			Pcap.findAllDevs(alldevs, errbuf);
			device = alldevs.get(ethDevNumber);
			
			Console("Opening '" + device.getName() + "' for traffic capture.", true);
			pb = new ProcessBuilder(new String[] { "tshark", "-i", device.getName(), "-f", "tcp dst port 80 or udp src port 5353 or udp src port 138", "-T", "fields", "-e", "eth.src", "-e", "wlan.sa", "-e", "ip.src", "-e", "tcp.srcport", "-e", "tcp.dstport", "-e", "udp.srcport", "-e", "udp.dstport", "-e", "browser.command", "-e", "browser.server", "-e", "dns.resp.name", "-e", "http.host", "-e", "http.request.uri", "-e", "http.user_agent", "-e", "http.referer", "-e", "http.cookie" } );
			pb.redirectErrorStream(true);
			deviceCaptureProcess[ethDevNumber] = pb.start();
			pw = new ProcessWatcher(deviceCaptureProcess[ethDevNumber]);
			is = deviceCaptureProcess[ethDevNumber].getInputStream();
		}
		else
		{
			Console("Opening '" + pcapFile + "' for traffic capture.", true);
			pb = new ProcessBuilder(new String[] { "tshark", "-r", pcapFile, "-T", "fields", "-e", "eth.src", "-e", "wlan.sa", "-e", "ip.src", "-e", "tcp.srcport", "-e", "tcp.dstport", "-e", "udp.srcport", "-e", "udp.dstport", "-e", "browser.command", "-e", "browser.server", "-e", "dns.resp.name", "-e", "http.host", "-e", "http.request.uri", "-e", "http.user_agent", "-e", "http.referer", "-e", "http.cookie" } );
			pb.redirectErrorStream(true);
			proc = pb.start();
			pw = new ProcessWatcher(proc);
			is = proc.getInputStream();
		}
		
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr, 2048);

		String line = "";
		try
		{
			while ((line = br.readLine()) != null || pw.isFinished())
			{
				if(line == null) // Can happen while loading in and the proc finishes faster than readin
					break;
				
				if(line.contains("\t"))
				{
					String[] values = line.split("\t", -1); // -1 limit means don't ignore whitespace
					
					if(values.length > 4) // If only contains four or less values, likely continuation data, ignore.
					{				
						//"eth.src", "-e", "wlan.sa", "-e", "ip.src", "-e", "tcp.srcport", "-e", "tcp.dstport", "udp.srcport", "-e", "udp.dstport", "-e", "browser.command", "-e", "browser.server", "-e", "dns.resp.name", "-e", "http.host", "-e", "http.request.uri", "-e", "http.user_agent", "-e", "http.referer", "-e", "http.cookie"
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
						String bonjourName = values[9];
						String requestHost = values[10];
						String requestURI = values[11];
						String userAgent = values[12];
						String referrerURI = values[13];
						String cookieData = values[14];
						
						// Poor man's implementation of a packet filter for when pcaps are loaded
						if(!pcapFile.isEmpty() && (!tcpDestination.equals("80") && !udpSource.equals("5353") && !udpSource.equals("138")))
							continue;
						
						if(!requestURI.isEmpty())
						{
							ProcessRequest(macAddress, ipAddress, requestHost, requestURI, userAgent, referrerURI, cookieData);
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
									
									macListModel.addElement(macAddress);
								}
								
								// And update the informational display
								DisplayInfoForMac(macAddress);
							}
							catch (SQLException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						else if(!bonjourName.isEmpty())
						{
							if(bonjourName.contains(","))
							{
								String[] bonjourResponses = bonjourName.split(",");
								String bonjourNameStr = bonjourResponses[bonjourResponses.length - 1];
								
								if(!bonjourNameStr.contains("_tcp") && !bonjourNameStr.contains("_udp") && !bonjourNameStr.contains("<Root>"))
								{									
									bonjourNameStr = bonjourNameStr.replace(".local", "");
									
									try
									{
										if(dbInstance.containsValue("clients", "mac", macAddress))
										{
											// We've seen this mac already, just set the hostname
											dbInstance.setStringValue("clients", "mdns_hostname", bonjourNameStr, "mac", macAddress);
										}
										else // Client object doesn't exist for MAC? Create one.
										{
											dbInstance.createClient(macAddress, ipAddress);
											dbInstance.setStringValue("clients", "mdns_hostname", bonjourNameStr, "mac", macAddress);
											
											macListModel.addElement(macAddress);
										}
										
										// And update the informational display
										DisplayInfoForMac(macAddress);
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
			if(!pcapFile.isEmpty() && proc != null)
			{
				proc.destroy();
			}
			else
			{
				if(deviceCaptureProcess[ethDevNumber] != null)
					deviceCaptureProcess[ethDevNumber].destroy();
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
			Console(device.getName() + ": " + "============================================================================", true);
			Console(device.getName() + ": " + "Start of diagnostic information for interface '" + device.getName() + "'", true);
			Console(device.getName() + ": " + "============================================================================", true);
			
			for (String output : nonTabbedOutput)
			{
				Console(device.getName() + ": " + output, true);				
			}
			
			Console(device.getName() + ": " + "============================================================================", true);
			Console(device.getName() + ": " + "Potential error detected! Capture stopped / died in " + Integer.toString(runTimeInSeconds) + " seconds.", true);
			Console(device.getName() + ": " + "All messages from the 'tshark' program have been printed above to assist you in solving any errors.", true);
			Console(device.getName() + ": " + "============================================================================", true);
		}
		else
		{
			if(pcapFile.isEmpty())
				Console("'" + device.getName() + "' has been closed and is finished with traffic capture. Capture duration: " + Integer.toString(runTimeInSeconds) + " seconds.", true);
			else
				Console("'" + pcapFile + "' has finished processing. Processing duration: " + Integer.toString(runTimeInSeconds) + " seconds.", true);
		}
	}
	
	private void StopCapture(int ethDevNumber)
	{
		interfaceListModel.removeElementAt(ethDevNumber);
		interfaceListModel.insertElementAt(deviceName[ethDevNumber] + " [" + deviceDescription[ethDevNumber] + "]", ethDevNumber);
		((JComboBox)GetComponentByName("interfaceListComboBox")).setSelectedIndex(ethDevNumber);

		if (deviceCaptureProcess[ethDevNumber] != null)
		{
			deviceCaptureProcess[ethDevNumber].destroy();
		}
		
		bCapturing[ethDevNumber] = false;
		
		SetCaptureButtonText();
	}
	
	private void PrepCapture(int ethDevNumber)
	{
		interfaceListModel.removeElementAt(ethDevNumber);
		interfaceListModel.insertElementAt(deviceName[ethDevNumber] + " [" + deviceDescription[ethDevNumber] + "] (CURRENTLY CAPTURING)", ethDevNumber);
		((JComboBox)GetComponentByName("interfaceListComboBox")).setSelectedIndex(ethDevNumber);
		
		bCapturing[ethDevNumber] = true;
		
		SetCaptureButtonText();
	}
	
	private void SetCaptureButtonText()
	{
		int selection = ((JComboBox)GetComponentByName("interfaceListComboBox")).getSelectedIndex();
		if (selection >= 0)
		{
			((JButton)GetComponentByName("btnMonitorOnSelected")).setEnabled(true);
			
			if(bCapturing[selection])
			{
				((JButton)GetComponentByName("btnMonitorOnSelected")).setText("Stop Capture on " + deviceName[selection]);
			}
			else
			{
				((JButton)GetComponentByName("btnMonitorOnSelected")).setText("Start Capture on " + deviceName[selection]);
			}	
		}
	}
	
	private void ProcessRequest(String macAddress, String ipAddress, String requestHost, String requestURI, String userAgent, String referrerURI, String cookieData)
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
			}
			else // Client object doesn't exist for MAC? Create one.
			{
				clientID = dbInstance.createClient(macAddress, ipAddress);
				macListModel.addElement(macAddress);
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
				domainID = dbInstance.getIntegerValue("domains", "id", "name", requestHost); // In 'domains' get id where name == $val_of_domain_var
			}
			else // Domain object doesn't exist for this name? Create one.
			{
				// Create new domain
				domainID = dbInstance.createDomain(requestHost);
				
				// And display it, if appropriate to do so
				if(!macList.isSelectionEmpty() && macList.getSelectedValue().equals(macAddress))
				{
					domainListModel.addElement(requestHost);
				}
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try
		{
			requestID = dbInstance.createRequest(requestURI, userAgent, referrerURI, cookieData, domainID, clientID);
			
			// Update the requests list if necessary
			if(!domainList.isSelectionEmpty() && domainList.getSelectedValue().equals(requestHost))
			{
				Date now = new Date();	
				String dateString = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(now);

				String idFormatted = String.format("%4d", requestID);
				requestListModel.addElement("#" + idFormatted + ":   " + dateString + "   " + requestURI);
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void ChangeDomainList(String macAddress)
	{
		domainListModel.clear();
		requestListModel.clear();
		
		try {
			for (String s : dbInstance.getDomains(macAddress))
			{
			      domainListModel.addElement(s);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void ChangeRequestList(String macAddress, String uriHost)
	{	
		requestListModel.clear();
		
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

				int id = new Integer(ids.get(i));
				String idFormatted = String.format("%4d", id);
				requestListModel.addElement("#" + idFormatted + ":   " + dateString + "   " + uris.get(i));
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void DisplayInfoForMac(String macAddress)
	{
		// Not the selected item? Don't care.
		if(macList.getSelectedValue() == null || !macList.getSelectedValue().equals(macAddress))
			return;
		
		// Set some interesting notes
		try {
			String notesTxt = "IP Address: " + dbInstance.getStringValue("clients", "ip", "mac", macAddress);
			String netbiosHost = dbInstance.getStringValue("clients", "netbios_hostname", "mac", macAddress);
			String mdnsHost = dbInstance.getStringValue("clients", "mdns_hostname", "mac", macAddress);
			
			if(!netbiosHost.isEmpty())
			{
				notesTxt = notesTxt + "\n" + "Host Name (NetBIOS): " + netbiosHost;
			}
			
			if(!mdnsHost.isEmpty())
			{
				notesTxt = notesTxt + "\n" + "Host Name (mDNS/Bonjour): " + mdnsHost;
			}
			
			txtInformation.setText(notesTxt);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		txtInformation.setCaretPosition(0); // Show the top
	}
	
	private void DisplayInfoForRequest(int request)
	{		
		if(requestList.getSelectedValue() == null)
			return;
		
		// Set some interesting notes
		try {
			String notesTxt = "URI: " + dbInstance.getStringValue("requests", "uri", "id", Integer.toString(request));
			String userAgent = dbInstance.getStringValue("requests", "useragent", "id", Integer.toString(request));
			String referrer = dbInstance.getStringValue("requests", "referrer", "id", Integer.toString(request));
			String cookies = dbInstance.getStringValue("requests", "cookies", "id", Integer.toString(request));
			
			if(!userAgent.isEmpty())
			{
				notesTxt = notesTxt + "\n" + "User Agent: " + userAgent;
			}
			
			if(!referrer.isEmpty())
			{
				notesTxt = notesTxt + "\n" + "Referer: " + referrer;
			}
			
			if(!referrer.isEmpty())
			{
				notesTxt = notesTxt + "\n" + "Cookies: " + cookies;
			}
			
			txtInformation.setText(notesTxt);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		txtInformation.setCaretPosition(0); // Show the top
	}
	
	// Now we're at the good part.
	private void LoadCookiesIntoBrowser(String domain, String uri, String useragent, String referrer, String cookies, boolean bReplay)
	{
		// We should start with a fresh driver every time, hence this stuff.
		try
		{
			driver.close();
		}
		catch (Exception e)
		{}
		finally
		{
			driver = null;
		}
		
		if(bReplay && !useragent.isEmpty())
		{
	        FirefoxProfile profile = new FirefoxProfile();
	        profile.setPreference("general.useragent.override", useragent);
			driver = new FirefoxDriver(profile);
		}
		else
		{
			driver = new FirefoxDriver();
		}
        
        driver.get("http://" + domain + "/__");
        driver.manage().deleteAllCookies();
        
        if(!cookies.isEmpty())
        {
	        String[] cookiesArray = cookies.split(";");
	        for (String individualCookieInfo : cookiesArray)
	        {
	        	String[] cookieData = individualCookieInfo.split("=");
	        	Cookie aCookie = new Cookie(cookieData[0], cookieData[1]);
	        	driver.manage().addCookie(aCookie);
	        }
        }
        
        driver.get("http://" + domain + uri);
	}
	
	public CookieCadgerInterface() throws Exception
	{
		setResizable(false);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e)
			{
				super.windowClosing(e);
				
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
		});	    
		dbInstance = new Sqlite3DB();
		
		setTitle("Cookie Cadger");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 950, 650);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		macListModel = new DefaultListModel();
		domainListModel = new DefaultListModel();
		requestListModel = new DefaultListModel();
		interfaceListModel = new DefaultComboBoxModel();
		
		// Get all packet capture devices
		List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with NICs
		StringBuilder errbuf = new StringBuilder();     // For any error msgs
		
		int r = Pcap.findAllDevs(alldevs, errbuf);
		if (r == Pcap.NOT_OK || alldevs.isEmpty())
		{
			if(errbuf.toString().isEmpty())
				JOptionPane.showMessageDialog(null, "Can't read list of devices. Program must run as root to be able to iterate capture devices.");
			else
				JOptionPane.showMessageDialog(null, "Can't read list of devices, error is: " + errbuf.toString());
		}

		deviceCount = alldevs.size();
		bCapturing = new Boolean[deviceCount];
		deviceName = new String[deviceCount];
		deviceDescription = new String[deviceCount];
		deviceCaptureProcess = new Process[deviceCount];
		
		int itemToSelect = -1;
		int i = 0;
		for (PcapIf device : alldevs)
		{
			bCapturing[i] = false;
			
			boolean bIsMon = false;
			String interfaceDesc = "no description";
			String interfaceName = deviceName[i] = device.getName();
			String interfaceText;
			
			if(device.getDescription() != null && !device.getDescription().isEmpty())
			{
				interfaceDesc = device.getDescription();
			}
			
			if(interfaceName.contains("mon0"))
			{
				bIsMon = true;
			}
			
			deviceDescription[i] = interfaceDesc;
			
			interfaceText = interfaceName + " [" + interfaceDesc + "]";
			interfaceListModel.addElement(interfaceText);
			
			if(bIsMon)
				itemToSelect = i;
			
			i++;
		}
		
		txtConsole = new JTextArea();
		txtConsole.setBackground(UIManager.getColor("Panel.background"));
		txtConsole.setFont(new Font("Verdana", Font.BOLD, 12));
		txtConsole.setEditable(false);
		
		consoleScrollPane = new JScrollPane();
		consoleScrollPane.setBounds(28, 530, 895, 75);
		contentPane.add(consoleScrollPane);
		consoleScrollPane.setViewportView(txtConsole);
		
		JButton btnMonitorOnSelected = new JButton("Select An Interface");
		btnMonitorOnSelected.setEnabled(false);
		btnMonitorOnSelected.setName("btnMonitorOnSelected");
		btnMonitorOnSelected.setBounds(510, 0, 240, 25);
		contentPane.add(btnMonitorOnSelected);
		
		JComboBox interfaceListComboBox = new JComboBox();
		interfaceListComboBox.setName("interfaceListComboBox");
		interfaceListComboBox.setBounds(28, 14, 460, 24);
		contentPane.add(interfaceListComboBox);
		
		interfaceListComboBox.setModel(interfaceListModel);
		interfaceListComboBox.setSelectedIndex(itemToSelect);
		
		JScrollPane macListScrollPanel = new JScrollPane();
		macListScrollPanel.setBounds(28, 62, 162, 382);
		contentPane.add(macListScrollPanel);
		
		macList = new ZebraJList();
		macList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		macList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if(!e.getValueIsAdjusting() && !macList.isSelectionEmpty())
				{
					JList list = (JList)e.getSource();
					String item = (String)list.getSelectedValue();
					ChangeDomainList(item);
					DisplayInfoForMac(item);
				}
			}
		});

		macListScrollPanel.setViewportView(macList);
		SortedListModel macListModelSorted = new SortedListModel(macListModel);
		macListModelSorted.setSortOrder(SortOrder.ASCENDING);
		macList.setModel(macListModelSorted);
		
		JScrollPane domainListScrollPane = new JScrollPane();
		domainListScrollPane.setBounds(202, 62, 200, 382);
		contentPane.add(domainListScrollPane);
		
		domainList = new ZebraJList();
		domainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		domainList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if(!e.getValueIsAdjusting() && !macList.isSelectionEmpty() && !domainList.isSelectionEmpty())
				{
					JList list = (JList)e.getSource();
					String item = (String)list.getSelectedValue();
					String macAddress = (String)macList.getSelectedValue();
					ChangeRequestList(macAddress, item);
				}
			}
		});
		
		domainListScrollPane.setViewportView(domainList);
		SortedListModel domainListModelSorted = new SortedListModel(domainListModel);
		domainListModelSorted.setSortOrder(SortOrder.ASCENDING);
		domainList.setModel(domainListModelSorted);
		
		JScrollPane requestListScrollPanel = new JScrollPane();
		requestListScrollPanel.setBounds(414, 62, 509, 308);
		contentPane.add(requestListScrollPanel);
		
		requestList = new ZebraJList();
		Font fontMonospace = new Font( "Monospaced", Font.BOLD, 12 ); 
		requestList.setFont(fontMonospace);
		requestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		requestList.setModel(requestListModel);
		requestList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if(!e.getValueIsAdjusting() && !macList.isSelectionEmpty() && !domainList.isSelectionEmpty() && !requestList.isSelectionEmpty())
				{
					String request = (String)requestList.getSelectedValue();
					int requestID = Integer.parseInt(request.substring(1, request.indexOf(":")).trim());
					DisplayInfoForRequest(requestID);
				}
			}
		});
		requestListScrollPanel.setViewportView(requestList);
		
		JButton btnLoadDomainCookies = new JButton("Load Domain Cookies");
		btnLoadDomainCookies.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0)
			{
				if(!domainList.isSelectionEmpty())
				{
					Thread loadCookiesThread = new Thread(new Runnable()
					{
						public void run()
						{
							try {
								int domainID = dbInstance.getIntegerValue("domains", "id", "name", (String)domainList.getSelectedValue());
								int requestID = dbInstance.getNewestRequestID(domainID);

								String domain = (String)domainList.getSelectedValue();
								String uri = "";
								String useragent = "";
								String referrer = "";
								String cookies = dbInstance.getStringValue("requests", "cookies", "id", Integer.toString(requestID));
								
								LoadCookiesIntoBrowser(domain, uri, useragent, referrer, cookies, false);
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
		btnLoadDomainCookies.setBounds(414, 381, 252, 25);
		contentPane.add(btnLoadDomainCookies);
		
		JButton btnReplayRequest = new JButton("Replay This Request");
		btnReplayRequest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0)
			{
				if(!requestList.isSelectionEmpty())
				{					
					Thread loadCookiesThread = new Thread(new Runnable()
					{
						public void run()
						{
							String request = (String)requestList.getSelectedValue();
							int requestID = Integer.parseInt(request.substring(1, request.indexOf(":")).trim());
							
							//uri VARCHAR, useragent VARCHAR, referrer VARCHAR, cookies VARCHAR, domain_id INTEGER, client_id INTEGER);");
							try {
								String domain = (String)domainList.getSelectedValue();
								String uri = dbInstance.getStringValue("requests", "uri", "id", Integer.toString(requestID));
								String useragent = dbInstance.getStringValue("requests", "useragent", "id", Integer.toString(requestID));
								String referrer = dbInstance.getStringValue("requests", "referrer", "id", Integer.toString(requestID));
								String cookies = dbInstance.getStringValue("requests", "cookies", "id", Integer.toString(requestID));
								
								LoadCookiesIntoBrowser(domain, uri, useragent, referrer, cookies, true);
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
		btnReplayRequest.setBounds(670, 381, 252, 25);
		contentPane.add(btnReplayRequest);
		
		JButton btnCopyInfo = new JButton("Copy Request To Clipboard");
		btnCopyInfo.setBounds(414, 418, 252, 25);
		contentPane.add(btnCopyInfo);
		
		JButton btnCopyAllRequests = new JButton("Copy All Requests to Clipboard");
		btnCopyAllRequests.setBounds(670, 418, 252, 25);
		contentPane.add(btnCopyAllRequests);
		
		informationScrollPane = new JScrollPane();
		informationScrollPane.setBounds(28, 450, 895, 75);
		contentPane.add(informationScrollPane);
		
		txtInformation = new JTextArea();
		txtInformation.setBackground(UIManager.getColor("Panel.background"));
		txtInformation.setFont(new Font("Dialog", Font.BOLD, 14));
		txtInformation.setText("Cookie Cadger\nCreated by Matthew Sullivan - mattslifebytes.com\nThis software is freely distributed under the FreeBSD license.");
		txtInformation.setLineWrap(true);
		txtInformation.setEditable(false);
		informationScrollPane.setViewportView(txtInformation);
		
		JButton btnOpenPcap = new JButton("Open a Capture File (*.pcap)");
		btnOpenPcap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0)
			{            
				Thread captureThread = new Thread(new Runnable()
				{
					public void run()
					{
						JFileChooser fc = new JFileChooser();
						FileFilter pcapFilter = new FileNameExtensionFilter("*.pcap | *.cap", "pcap", "cap");
						fc.addChoosableFileFilter(pcapFilter);
						fc.setFileFilter(pcapFilter);

						int returnVal = fc.showOpenDialog(contentPane);

				        if (returnVal == JFileChooser.APPROVE_OPTION)
				        {
				            File file = fc.getSelectedFile();
				            JOptionPane.showMessageDialog(contentPane, "This process could take some time, please be patient.");
						            
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
		btnOpenPcap.setBounds(510, 30, 240, 25);
		contentPane.add(btnOpenPcap);
		
		/*
		 // MANUAL START OF CAPTURE.... HORRIBLE TODO

		System.out.println("starting fork...");
		final CookieCadgerInterface me = this;
		Thread captureThread = new Thread(new Runnable() {
			public void run()
			{
				try {
					System.out.println("capture call...");
					me.Capture(4);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		System.out.println("starting thread...");
		captureThread.start();
		*/
		
		CreateComponentMap();
		
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
				boolean bInterfaceIsCapturing = bCapturing[selectedInterface]; // Make a copy because this value will be changing a lot...
							
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
		
		btnCopyInfo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0)
			{
				if(requestList.isSelectionEmpty())
				{
					JOptionPane.showMessageDialog(contentPane, "You must first select a request.");
				}
				else
				{
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Clipboard clipboard = toolkit.getSystemClipboard();
					StringSelection strSel = new StringSelection((String)requestList.getSelectedValue());
					clipboard.setContents(strSel, null);
				}
			}
		});
		
		btnCopyAllRequests.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				String allRequests = "";
				
				for (int i = 0; i < requestListModel.getSize(); i++)
				{
					allRequests = allRequests + requestListModel.get(i) + "\n";
				}
				
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Clipboard clipboard = toolkit.getSystemClipboard();
				StringSelection strSel = new StringSelection(allRequests);
				clipboard.setContents(strSel, null);
			}
		});
	}
	
	private void Console(String text, boolean bAutoScroll)
	{
		if(txtConsole.getText().isEmpty())
			txtConsole.setText(text);
		else
			txtConsole.setText(txtConsole.getText() + "\n" + text);
		
		if(bAutoScroll)
			txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
		
		System.out.println(text);
	}
	
	private void CreateComponentMap() {
        componentMap = new HashMap<String,Component>();
        Component[] components = this.contentPane.getComponents();
        for (int i=0; i < components.length; i++) {
                componentMap.put(components[i].getName(), components[i]);
        }
	}

	public Component GetComponentByName(String name) {
        if (componentMap.containsKey(name)) {
                return (Component) componentMap.get(name);
        }
        else return null;
	}
}