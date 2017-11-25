package lab10;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class PrimeNumGenN extends JFrame
{
	private static final long serialVersionUID = 1L;
	private final JTextArea aTextField = new JTextArea();
	private final JButton primeButton = new JButton("Start");
	private final JButton cancelButton = new JButton("Cancel");
	private volatile boolean cancel = false;
	private final PrimeNumGenN thisFrame;

	private static final Map<Integer, Integer> primeMap = new ConcurrentHashMap<Integer, Integer>();
	private static final Map<Integer, Integer> labelMap = new ConcurrentHashMap<Integer, Integer>();
	private static final int NUM_PROC = Runtime.getRuntime().availableProcessors();
	private static final CyclicBarrier barrier = new CyclicBarrier(NUM_PROC);
	long startTime;
	private Integer max;

	public static void main(String[] args)
	{
		PrimeNumGenN png = new PrimeNumGenN("Primer Number Generator");
		// don't add the action listener from the constructor
		png.addActionListeners();
		png.setVisible(true);

	}

	private PrimeNumGenN(String title)
	{
		super(title);
		this.thisFrame = this;
		cancelButton.setEnabled(false);
		aTextField.setEditable(false);
		setSize(200, 200);
		setLocationRelativeTo(null);
		// kill java VM on exit
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(primeButton, BorderLayout.SOUTH);
		getContentPane().add(cancelButton, BorderLayout.EAST);
		getContentPane().add(new JScrollPane(aTextField), BorderLayout.CENTER);
	}

	private class CancelOption implements ActionListener
	{
		public void actionPerformed(ActionEvent arg0)
		{
			cancel = true;
		}
	}

	private void addActionListeners()
	{
		cancelButton.addActionListener(new CancelOption());

		primeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{

				String num = JOptionPane.showInputDialog("Enter a large integer");
				// Integer max = null;
				try
				{
					max = Integer.parseInt(num);
				} catch (Exception ex)
				{
					JOptionPane.showMessageDialog(thisFrame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}

				if (max != null)
				{
					aTextField.setText("");
					primeButton.setEnabled(false);
					cancelButton.setEnabled(true);
					cancel = false;
					primeMap.clear();
					labelMap.clear();
					new Thread(new UserInput()).start();
					startTime = System.currentTimeMillis();
				}
			}
		});
	}

	private boolean isPrime(int i)
	{
		for (int x = 2; x < i - 1; x++)
			if (i % x == 0)
				return false;

		return true;
	}

	public class Worker implements Runnable
	{
		private final Semaphore semaphore;
		private final int num;

		public Worker(Semaphore semaphore, int num)
		{
			this.semaphore = semaphore;
			this.num = num;
		}

		@Override
		public void run()
		{
			long lastUpdate = System.currentTimeMillis();
			try
			{
				for (int i = num; i <= max && !cancel; i = i + NUM_PROC)
				{
					labelMap.put(i, 1);
					if (isPrime(i))
					{
						primeMap.put(i, 1);

						if (System.currentTimeMillis() - lastUpdate > 500)
						{
							final String outString = "Found " + primeMap.size() + " in " + labelMap.size() + " of "
									+ max;
							SwingUtilities.invokeLater(new Runnable()
							{
								@Override
								public void run()
								{
									aTextField.setText(outString);
								}
							});
							lastUpdate = System.currentTimeMillis();
						}
					}
				}
				// wait for all threads
				barrier.await();
			} catch (Exception ex)
			{
				ex.printStackTrace();
			} finally
			{
				semaphore.release();
			}
		}
	}

	private class UserInput implements Runnable
	{
		public void run()
		{
			Semaphore semaphore = new Semaphore(NUM_PROC);
			for (int x = 0; x < NUM_PROC; x++)
			{
				try
				{
					semaphore.acquire();
				} catch (Exception ex)
				{
					ex.printStackTrace();
				}
				// add a worker here
				Worker w = new Worker(semaphore, x + 1);
				new Thread(w).start();
			}
			for (int x = 0; x < NUM_PROC; x++)
			{
				try
				{
					semaphore.acquire();
				} catch (InterruptedException e1)
				{
					e1.printStackTrace();
				}
			}
			// get the final output
			final StringBuffer buff = new StringBuffer();
			SortedSet<Integer> keys = new TreeSet<Integer>(primeMap.keySet());
			// Uncomment next two commented lines to store primes in a list
			// List<Integer> list = new ArrayList<Integer>();
			for (Integer key : keys)
			{
				buff.append(key + "\n");
				// list.add(key)
			}
			buff.append("Time " + (System.currentTimeMillis() - startTime) / 1000f + "S" + "\n");
			if (cancel)
				buff.append("cancelled");
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					cancel = false;
					primeButton.setEnabled(true);
					cancelButton.setEnabled(true);
					aTextField.setText((cancel ? "cancelled " : "") + buff.toString());
					buff.setLength(0);
				}
			});
		}// end run
	} // end UserInput
}
