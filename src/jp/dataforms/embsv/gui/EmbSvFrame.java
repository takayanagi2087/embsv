package jp.dataforms.embsv.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class EmbSvFrame extends JFrame {

	private static final String SYSTEM_NAME = "Embedded Server";


	private static final long serialVersionUID = 1L;


	/**
	 * リソース。
	 */
	private static ResourceBundle resource = ResourceBundle.getBundle("jp.dataforms.embsv.gui.EmbSvFrame");

	
	/**
	 * アイコンイメージ。
	 */
	private Image iconImage = null;

	/**
	 * アプリケーション。
	 */
	private JList<String> appList = null;

	
	/**
	 * タスクトレイアイコン。
	 */
	private TrayIcon icon = null;
	
	/**
	 * 内容表示領域。
	 */
	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EmbSvFrame frame = new EmbSvFrame();
					frame.setTaskTrayIcon();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public EmbSvFrame() throws Exception {
		setTitle(SYSTEM_NAME);
		this.iconImage = ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("embsv.png"));
		this.setIconImage(this.iconImage);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.setFrameMenu();
		
		this.setBounds(100, 100, 346, 237);
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.contentPane.setLayout(new BorderLayout(0, 0));
		this.setContentPane(this.contentPane);

		// タイトルパネル
		{
			JPanel titlePanel = new JPanel();
			this.contentPane.add(titlePanel, BorderLayout.NORTH);
			JLabel label = new JLabel(EmbSvFrame.resource.getString("menuitem.applist"));
			label.setHorizontalAlignment(SwingConstants.CENTER);
			titlePanel.add(label);

		}
		// アプリケーションリスト
		{
			
			this.appList = new JList<String>();
			JScrollPane scrollPane = new JScrollPane(this.appList);
/*			appList.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						try {
							int idx = appList.getSelectedIndex();
							@SuppressWarnings("unchecked")
							List<Map<String, Object>> list = (List<Map<String, Object>>) config.get("webapps");
							Map<String, Object> m = list.get(idx);
							RunWar.this.runBrowser(m);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			});*/
			this.contentPane.add(scrollPane, BorderLayout.CENTER);
		}
		// ボタンパネル
		{
			JPanel buttonPanel = new JPanel();
			this.contentPane.add(buttonPanel, BorderLayout.SOUTH);
			JButton closeButton = new JButton(EmbSvFrame.resource.getString("button.close"));
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
	/*				if (MODE_TASKTRAY.equals(RunWar.this.getMode())) {
						RunWar.this.setVisible(false);
					} else {
						RunWar.this.stop();
						System.exit(0);
					}*/
				}
			});
			buttonPanel.add(closeButton);
		}

	}
	
	/**
	 * ウインドウのメニューを設定します。
	 */
	private void setFrameMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu(EmbSvFrame.resource.getString("menu.file"));
		JMenuItem exitItem = new JMenuItem(EmbSvFrame.resource.getString("menuitem.exit"));
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//RunWar.this.stop();
				System.exit(0);
			}
		});
		fileMenu.add(exitItem);
		JMenu helpMenu = new JMenu(EmbSvFrame.resource.getString("menu.help"));
		JMenuItem aboutItem = new JMenuItem(EmbSvFrame.resource.getString("menuitem.about"));
		aboutItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//EmbSvFrame.this.about();
			}
		});
		helpMenu.add(aboutItem);
		menuBar.add(fileMenu);
		menuBar.add(helpMenu);
		this.setJMenuBar(menuBar);
	}

	/**
	 * タスクトレイを設定します。
	 * @throws Exception 例外。
	 */
	private void setTaskTrayIcon() throws Exception {
		// トレイアイコン生成
		Dimension d = SystemTray.getSystemTray().getTrayIconSize();
		Image img = this.iconImage.getScaledInstance((int) d.getWidth() - 2, (int) d.getHeight() - 2, Image.SCALE_SMOOTH);
		this.icon = new TrayIcon(img, SYSTEM_NAME);
		// イベント登録
		icon.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EmbSvFrame.this.setVisible(true);
			}
		});
		// ポップアップメニュー
		PopupMenu menu = new PopupMenu();
		// メニューの例
		MenuItem appList = new MenuItem(EmbSvFrame.resource.getString("menuitem.applist"));
		appList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EmbSvFrame.this.setVisible(true);
			}
		});

		MenuItem about = new MenuItem(EmbSvFrame.resource.getString("menuitem.about"));
		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// EmbSvFrame.this.about();
			}
		});

		// 終了メニュー
		MenuItem exitItem = new MenuItem(EmbSvFrame.resource.getString("menuitem.exit"));
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// EmbSvFrame.this.stop();
				System.exit(0);
			}
		});
		// メニューにメニューアイテムを追加
		menu.add(appList);
		menu.add(about);
		menu.add(exitItem);
		icon.setPopupMenu(menu);

		// タスクトレイに格納
		SystemTray.getSystemTray().add(icon);
	}


}
