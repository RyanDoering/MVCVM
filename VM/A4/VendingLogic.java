package Assignment4;

import org.lsmr.vending.*;
import org.lsmr.vending.hardware.*;
import org.lsmr.vending.hardware.CapacityExceededException;
import org.lsmr.vending.hardware.DisabledException;
import org.lsmr.vending.hardware.EmptyException;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class VendingLogic implements VendingLogicInterface {
	private VendingMachine vm; // The vending machine that this logic program is installed on
	private int credit; // credit is saved in terms of cents
	private EventLogInterface EL; // An even logger used to track vending machine interactions
	private Boolean[] circuitEnabled; // an array used for custom configurations
	private boolean debug = false;
	private String currentMessage = "";
	private Timer timer1;
	private Timer timer2;

	private ConfigPanel configPanel;
	private LockLogic lock;

	public boolean cardEnabled = true; // For enabling credit card purchases, on by default (NEW)

	private final double bitCoinExchangeRate = 12383.43;
	private boolean enableBitCoin = false;
	// private VendingBTChardware tempBTChardware; //We create a temporary hardware
	// piece in lue of the actual hardware.

	public boolean displayWelcome;
	boolean configMode = false;
	boolean mode1 = false;
	boolean mode2 = false;
	boolean enterButton = false;
	private String selected = "";
	private ArrayList<Integer> popCosts;
	private int setPrice;
	private int indexSelected = -1;
	MyControlDialog C;

	/**
	 * This constructor uses a vending machine as a parameter, then creates and
	 * assigns listeners to it.
	 *
	 * @param VendingMachine
	 *            vend is the the machine that the listeners will be registered to.
	 * @return a new instance of a VendingLogic object
	 *
	 */
	public VendingLogic(VendingMachine vend, MyControlDialog Control) {
		// Set up attributes
		this.C = Control;
		this.vm = vend;
		credit = 0;
		EL = new EventLog();

		// if(enableBitCoin) {
		// tempBTChardware = BitCoinListener.tempCreateHardware();
		// }
		registerListeners();

		popCosts = new ArrayList<Integer>(vm.getNumberOfPopCanRacks());

		configPanel = new ConfigPanel(vend); // instantiates config panel

		// Set up the custom configuration
		circuitEnabled = new Boolean[vm.getNumberOfSelectionButtons()];
		for (int i = 0; i < circuitEnabled.length; i++) {
			circuitEnabled[i] = true; // we enable all by default
		}
		this.welcomeMessageTimer();
		C.DisplayField.setText("hello");

		this.lock = new LockLogic(vend, configPanel); // instantiates lock, DEFAULT is UNLOCKED, must LOCK to access
														// config
														// panel
	}

	/**
	 * Method handles bitcoin transactions, using the temporary hardware
	 * 
	 * @param none
	 * @return none
	 */
	// public void bitcoinTransaction() {
	// // System.out.println("BTC transaction!");
	// if (verifyBTCuser(tempBTChardware.getUserID())) {
	// if
	// (tempBTChardware.getBTCpaymentMethods().contains(tempBTChardware.getPaymentType()))
	// {
	//
	// // Replace this with proper BTC payment protocol
	// // For now just give it to them as credit.
	// // TODO impliment handling for charging the user.
	// /*
	// * if(verifyBtUser(gui_view.getBTCUser())){ if(tempBTChardware.getBCT() >=
	// * convertCADtoBTC(selected.getPrice())) {
	// * tempBTChardware.chargeBTC(convertCADtoBTC(selected.getPrice()));
	// * vm.getPopCanRack(index).dispensePopCan();
	// * vm.getDisplay().display("Enjoy your drink!");
	// *
	// * } else
	// * vm.getDisplay().display("You don't have enough Bitcoin, go back to
	// mining!");
	// * } else vm.getDisplay().display("Username and Pin don't match");
	// */
	// } else
	// vm.getDisplay().display("Invalid payment type.");
	//
	// } else
	// vm.getDisplay().display("Username and Pin don't match");
	//
	// }

	// private boolean verifyBTCuser(int userID) {
	// // When the bitcoin system is created, replace this method with the user
	// // verifcation protocoll.
	// return true;
	// }

	public double convertCADtoBTC(int cad) {
		if (cad < 0)
			throw new IllegalStateException("Unable to convert negative CAD values");

		double caddouble = cad / 100;
		return caddouble / bitCoinExchangeRate;
	}

	public int convertBTCtoCAD(double btc) {
		if (btc < 0)
			throw new IllegalStateException("Unable to convert negative bitcoin values");
		int cad = (int) Math.floor(btc * bitCoinExchangeRate);
		return cad;
	}

	/*
	 * RYAN, Added Methods for checking a card, and then processing payment. Handles
	 * if there is already coins in the machine and if only paying by card. Gives
	 * out messages to the display for most of the errors
	 * 
	 * @param index indeex of the Button Pressed
	 */
	public void checkPayByCard(Card card, int index)
			throws DisabledException, EmptyException, CapacityExceededException {
		if (cardEnabled) {
			if (Card.getAcceptedBanks().contains(card.getBankName())) {
				if (!(card.getCardType() == "Invalid")) // check if the card is Credit or Debit
				{
					// Then the bank type can be used in the vending machine system
					if (card.getCardBalance() > 0) // if the balance is more than 0, then go to try to pay with the card
					{
						setCurrentMessage("Paying with " + card.getBankName() + " " + card.getCardType() + " card");
						C.DisplayField.setText(getCurrentMessage());
						vm.getDisplay()
								.display("Paying with " + card.getBankName() + " " + card.getCardType() + " card");
						payByCard(card, index); // message of what type of card they are paying with, then proceeds to
												// pay with that card
					}
				}
			} else {
				setCurrentMessage("Card not valid");
				C.DisplayField.setText(getCurrentMessage());
				vm.getDisplay().display("Card not valid");
				this.purchasedByCard(false, card);
			}
		} else {
			setCurrentMessage("Credit or debit cards are not accepted");
			C.DisplayField.setText(getCurrentMessage());
			vm.getDisplay().display("Credit or debit cards are not accepted"); // if the cardEnabled is turned off
			this.purchasedByCard(false, card);
		}
	}

	public void payByCard(Card card, int index) throws DisabledException, EmptyException, CapacityExceededException {
		double price = (double) vm.getPopKindCost(index) / 100;
		double funds = card.getCardBalance(); // total funds on that card
		double thisCredit = (double) credit / 100;
		if (credit > 0) // if there are coins in the machine
		{
			price = price - thisCredit; // subtracts the coins which are already in the machine from the price
			System.out.println(funds);
			System.out.println(price);
			if (funds < price) {
				setCurrentMessage("Card has insufficient funds");
				C.DisplayField.setText(currentMessage);
				vm.getDisplay().display("Card has insufficient funds");
				this.purchasedByCard(false, card);
				price = price + thisCredit; // set the price back to normal
			} else {
				try {
					credit = 0;
					vm.getPopCanRack(index).dispensePopCan();
					funds = funds - price; // subtract the price payed from the funds of the card
					card.setNewBalance(funds); // set new the balance of the card after purchase
					this.purchasedByCard(true, card);
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		} else // credit is 0, no coins are in the machine, pay only by card
		{
			if (card.getCardBalance() >= price) {
				try {
					credit = 0;
					vm.getPopCanRack(index).dispensePopCan();
					funds = funds - price;
					card.setNewBalance(funds);
					this.purchasedByCard(true, card);
				} catch (Exception e) {
					System.out.println(e);
				}
			} else {
				setCurrentMessage("Card has insufficient funds");
				C.DisplayField.setText(currentMessage);
				vm.getDisplay().display("Card has insufficient funds");
				this.purchasedByCard(false, card);
			}
		}

	}
	/* END */

	/*
	 * Cynthia: Created new methods to: enable and disable card acceptor pay by
	 * tapping, wiping and inserting card and return card
	 */
	private CardAcceptor cardAcceptor = new CardAcceptor(); // Notice: this card acceptor has not been installed into
															// the vending machine.
	private boolean purchaseSucceeded = false; // A flag announcing whether the purchase is successful or not. In case
												// it'll be needed in the future.

	/**
	 * This method returns the vm field of the logic object
	 * 
	 * @param None
	 * @return this.vm
	 */
	public VendingMachine getVm() {
		return this.vm;
	}

	/**
	 * This method returns the purchaseSucceeded field of the logic object
	 * 
	 * @param None
	 * @return this.purchaseSucceeded
	 */
	public boolean getPurchaseSucceeded() {
		return this.purchaseSucceeded;
	}

	/**
	 * This method register the card acceptor in the hardware
	 * 
	 * @param None
	 * @return void
	 */
	public void registerCardAcceptor(CardAcceptorListenerDevice listener) {
		this.cardAcceptor.register(listener);
	}

	/**
	 * This method enables the card acceptor
	 * 
	 * @param None
	 * @return void
	 */
	public void enableCardAcceptor() {
		this.cardEnabled = true;
		vm.getDisplay().display("Card acceptor has been enabled.");
		vm.getDisplay().display("Tap/Swipe/Insert");
	}

	/**
	 * This method disables the card acceptor
	 * 
	 * @param None
	 * @return void
	 */
	public void disableCardAcceptor() {
		this.cardEnabled = false;
		vm.getDisplay().display("Card acceptor has been disabled.");
	}

	/**
	 * This method verifies if the purchase is successful
	 * 
	 * @param succeeded:
	 *            indicating if the payment is successful card: the paying card
	 * @return void
	 */
	public void purchasedByCard(boolean succeeded, Card card) throws DisabledException {
		if (succeeded) {
			this.purchaseSucceeded = true;
		} else {
			this.purchaseSucceeded = false;
		}

	}

	/**
	 * This method process paying by tapping cards
	 * 
	 * @param card:
	 *            the paying card index: the index of the pop
	 * @return void
	 */
	public void payByTappingCard(Card card, int index)
			throws DisabledException, EmptyException, CapacityExceededException {
		this.cardAcceptor.tapCard(card);
		checkPayByCard(card, index);
		if (this.purchaseSucceeded) {
			C.DisplayField.setText("Approved");
			vm.getDisplay().display("Approved.");
		} else {
			C.DisplayField.setText("Payment Failed. Try Again");
			vm.getDisplay().display("Payment failed. Try again.");
		}
	}

	/**
	 * This method process paying by wiping cards
	 * 
	 * @param card:
	 *            the paying card index: the index of the pop
	 * @return void
	 */
	public void payByWipingCard(Card card, int index)
			throws DisabledException, EmptyException, CapacityExceededException {
		this.cardAcceptor.swipeCard(card);
		checkPayByCard(card, index);
		if (this.purchaseSucceeded) {
			vm.getDisplay().display("Approved.");
		} else {
			vm.getDisplay().display("Payment failed. Try again.");
		}
	}

	/**
	 * This method process paying by inserting cards
	 * 
	 * @param card:
	 *            the paying card index: the index of the pop
	 * @return void
	 */
	public void payByInsertingCard(Card card, int index)
			throws DisabledException, EmptyException, CapacityExceededException {
		this.cardAcceptor.insertCard(card);
		checkPayByCard(card, index);
		if (this.purchaseSucceeded) {
			vm.getDisplay().display("Approved. Remove card.");
		} else {
			vm.getDisplay().display("Payment failed. Try again.");
		}
		this.cardAcceptor.returnCard(card);
	}

	/* END */

	/**
	 * This method returns the event logger
	 * 
	 * @param None
	 * @return EventLogInterface El
	 */
	public EventLogInterface getEventLog() {
		return EL;
	}

	/**
	 * This method returns the the credit total that the vending machine has
	 * 
	 * @param None
	 * @return Int credit
	 */
	public int getCurrencyValue() {
		return credit;
	}

	/**
	 * This method creates and registers listeners for the vending machine.
	 * 
	 * @param None
	 * @return None
	 */
	private void registerListeners() {
		// Register each of our listener objects here
		vm.getCoinSlot().register(new CoinSlotListenerDevice(this));
		vm.getDisplay().register(new DisplayListenerDevice(this));

		// For each coin rack create and register a listener
		for (int i = 0; i < vm.getNumberOfCoinRacks(); i++) {
			vm.getCoinRack(i).register(new CoinRackListenerDevice(this));
		}
		vm.getCoinReceptacle().register(new CoinReceptacleListenerDevice(this));

		// if (enableBitCoin) {
		// tempBTChardware.register(new BitCoinListener(this));
		// }

		try {
			vm.getCoinReturn().register(new CoinReturnListenerDevice(this));
		} catch (Exception e) {
			// This will print out the null pointer error
			if (debug)
				System.out.println("Coin return not instantiated! " + e);
		}

		// For each button create and register a listener
		for (int i = 0; i < vm.getNumberOfSelectionButtons(); i++) {
			vm.getSelectionButton(i).register(new PushButtonListenerDevice(this));
		}
		try {
			// Configuration Panel has 37 buttons. This is a hard coded value.
			for (int i = 0; i < 37; i++) {
				vm.getConfigurationPanel().getButton(i).register(new PushButtonListenerDevice(this));
			}

			vm.getConfigurationPanel().getEnterButton().register(new PushButtonListenerDevice(this));
		} catch (Exception e) {
			if (debug)
				System.out.println("Invalid config setup");
		}
		// For each pop rack create and register a listener
		for (int i = 0; i < vm.getNumberOfPopCanRacks(); i++) {
			vm.getPopCanRack(i).register(new PopCanRackListenerDevice(this));
		}
	}

	/**
	 * A method to begin the timers for the welcome message
	 */
	public void welcomeMessageTimer() {
		displayWelcome = true;
		timer1 = new Timer();
		timer1.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				welcomeMessage();
			}
		}, 0, 15000);

		timer2 = new Timer();
		timer2.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				clearDisplayMessage();
			}
		}, 5000, 15000);

	}

	/**
	 * A method to push a welcome message to the display
	 */
	public void welcomeMessage() {
		setCurrentMessage("Hi, There!");
		C.DisplayField.setText(getCurrentMessage());
		vm.getDisplay().display("Hi There!");
	}

	/**
	 * A method to clear the message to the display
	 */
	public void clearDisplayMessage() {
		setCurrentMessage("");
		C.DisplayField.setText(getCurrentMessage());
		vm.getDisplay().display("");
	}

	/**
	 * A method to send an OutOfOrder message to the display
	 */
	public void vendOutOfOrder() {
		try {
			timer1.cancel();
			timer2.cancel();
		} catch (Exception e) {
			// do nothing
		}
		setCurrentMessage("Out of Order");
		C.DisplayField.setText(getCurrentMessage());
		vm.getDisplay().display("Out Of Order");
	}

	/**
	 * A method to push the currently accumulated credit to the display
	 */
	public void displayCredit() {
		try {
			timer1.cancel();
			timer2.cancel();
		} catch (Exception e) {
			// do nothing
		}
		setCurrentMessage("Current Credit: $" + (((double) credit) / 100));
		C.DisplayField.setText(getCurrentMessage());
		vm.getDisplay().display("Current Credit: $" + (((double) credit) / 100));
	}

	/**
	 * A method to display the price of the pop at a specific index
	 * 
	 * @param index
	 *            - the selection number that corresponds to the desired pop
	 */

	public void displayPrice(int index) {
		try {
			timer1.cancel();
			timer2.cancel();
		} catch (Exception e) {
			// do nothing
		}
		setCurrentMessage("Price of " + vm.getPopKindName(index) + ": $" + (((double) vm.getPopKindCost(index)) / 100));
		C.DisplayField.setText(getCurrentMessage());
		vm.getDisplay()
				.display("Price of " + vm.getPopKindName(index) + ": $" + (((double) vm.getPopKindCost(index)) / 100));
		/*
		 * try { if (!debug) Thread.sleep(5000); // wait for 5 seconds } catch
		 * (InterruptedException e) { e.printStackTrace(); } if (credit == 0)
		 * welcomeMessageTimer(); else this.displayCredit();
		 */
	}

	/**
	 * Method to show that an invalid coin was inserted TODO is this an acceptible
	 * way to wait for 5 seconds?
	 */
	public void invalidCoinInserted() {
		try {
			timer1.cancel();
			timer2.cancel();
		} catch (Exception e) {
			// do nothing
		}
		setCurrentMessage("Invalid coin!");
		C.DisplayField.setText(getCurrentMessage());
		vm.getDisplay().display("Invalid coin!");
		try {
			if (!debug)
				Thread.sleep(5000); // wait for 5 seconds
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (credit == 0)
			welcomeMessageTimer();
		else
			this.displayCredit();
	}

	/**
	 * Method called by the coinSlotListener to accumulate credit when valid coins
	 * are inserted. Update the credit and update the display. Recalculate if the
	 * exact change is possible
	 * 
	 * @param coin
	 *            The Coin that was inserted
	 */
	public void validCoinInserted(Coin coin) {
		credit += coin.getValue();
		try {
			timer1.cancel();
			timer2.cancel();
		} catch (Exception e) {
			// do nothing
		}
		// Light the exact change light based on attempted change output
		if (!isExactChangePossible()) {
			vm.getExactChangeLight().activate();
			C.Cash.setText("ECO Light On");
		} else {
			vm.getExactChangeLight().deactivate();
			C.Cash.setText("ECO Light Off");
		}

		this.displayCredit();
	}

	/**
	 * Method to confirm that the product is being dispensed
	 */
	public void dispensingMessage() {
		setCurrentMessage("Despensing. Enjoy!");
		C.DisplayField.setText(getCurrentMessage());
		vm.getDisplay().display("Despensing. Enjoy!");
	}

	/**
	 * this method returns the current contents of the display
	 * 
	 * @param none
	 * @return String currentMessage
	 */
	public String getCurrentMessage() {
		return currentMessage;
	}

	/**
	 * this method sets the contents of the display, called by displayListenerDevice
	 * 
	 * @param String
	 *            x is the new message
	 * @return void
	 */
	public void setCurrentMessage(String x) {
		currentMessage = x;
	}

	/**
	 * A method to return change to the user
	 */
	public void returnChange(int CreditIn) {
		int nickelTotal = vm.getCoinRackForCoinKind(5).size();
		int dimeTotal = vm.getCoinRackForCoinKind(10).size();
		int quarterTotal = vm.getCoinRackForCoinKind(25).size();
		int loonieTotal = vm.getCoinRackForCoinKind(100).size();
		int toonieTotal = vm.getCoinRackForCoinKind(200).size();
		int changeRequired = credit;
		System.out.println("inCR"+changeRequired + vm.getCoinRackForCoinKind(200).size());

		while (changeRequired >= 200 && toonieTotal > 0) {
			changeRequired -= 200;
			toonieTotal--;
			decrementTotal(200);
			try {
				vm.getCoinRackForCoinKind(200).releaseCoin();
			} catch (CapacityExceededException | EmptyException | DisabledException e) {
				System.out.println(e);
			}
		}
		System.out.println("Toon" + changeRequired);
		while (changeRequired >= 100 && loonieTotal > 0) {
			changeRequired -= 100;
			loonieTotal--;
			decrementTotal(100);
			try {
				vm.getCoinRackForCoinKind(100).releaseCoin();
			} catch (CapacityExceededException | EmptyException | DisabledException e) {
				System.out.println(e);
			}		
		}
		System.out.println("Loon" + changeRequired);
		while (changeRequired >= 25 && quarterTotal > 0) {
			changeRequired -= 25;
			quarterTotal--;
			decrementTotal(25);
			try {
				vm.getCoinRackForCoinKind(25).releaseCoin();
			} catch (CapacityExceededException | EmptyException | DisabledException e) {
				System.out.println(e);
			}
		}
		while (changeRequired >= 10 && dimeTotal > 0) {
			changeRequired -= 10;
			dimeTotal--;
			decrementTotal(10);
			try {
				vm.getCoinRackForCoinKind(10).releaseCoin();
			} catch (CapacityExceededException | EmptyException | DisabledException e) {
				System.out.println(e);
			}
		}
		while (changeRequired >= 5 && nickelTotal > 0) {
			changeRequired -= 5;
			nickelTotal--;
			decrementTotal(5);
			try {
				vm.getCoinRackForCoinKind(5).releaseCoin();
			} catch (CapacityExceededException | EmptyException | DisabledException e) {
				System.out.println(e);
			}
		}

		System.out.println("The Fuck");

		/*
		 * if (vm.getCoinReturn() != null) { int[] coinKinds = getVmCoinKinds(); //
		 * vm.getCoinKindForCoinRack(0);// {200, 100, 25, 10, 5}; // legal value // of
		 * Canadian coins. only types returned for (int i = 0; i < coinKinds.length;
		 * i++) { CoinRack rack = vm.getCoinRackForCoinKind(coinKinds[i]); // the coin
		 * rack for the coin value indicated // by the loop if (rack != null) { // if
		 * rack = null. coin kind is not a valid change option while
		 * ((!vm.isSafetyEnabled()) && (credit > coinKinds[i]) && (!rack.isDisabled())
		 * && (rack.size() > 0)) { System.out.println("Didnt do shit got here"); try {
		 * 
		 * rack.releaseCoin(); credit -= coinKinds[i]; // subtracting (i) cents from the
		 * credit } catch (CapacityExceededException e) { // should never happen,
		 * receptacle full should enable the safety, which is in // the loop guard
		 * e.printStackTrace(); } catch (EmptyException e) { // should never happen,
		 * checked for in the loop guard e.printStackTrace(); } catch (DisabledException
		 * e) { // should never happen, checked for in the loop guard
		 * e.printStackTrace(); } } } } } else {
		 * System.out.println("Didnt do shit got here");
		 * setCurrentMessage("Unable to return any change");
		 * C.DisplayField.setText(getCurrentMessage());
		 * vm.getDisplay().display("Unable to return any change"); }
		 */

		if (!isExactChangePossible()) {
			C.Cash.setText("ECO Light On");
			vm.getExactChangeLight().activate();
		} else {
			vm.getExactChangeLight().deactivate();
			C.Cash.setText("ECO Light Off");
		}
	}

	public void decrementTotal(int price) throws SimulationException {
		System.out.println(credit+"indec");
		if (credit >= price) {
			credit -= price;
			System.out.println(credit);
			displayCredit();
		} else
			throw new SimulationException("Decrement cannot result in total being a negative value");
	}

	/**
	 * Method finds out what coin kinds are used in the vending machine based on the
	 * number of coin racks. This cannot be called while coinReturn is bugged
	 * 
	 * @return int[] coinKinds, for example {5, 10, 25, 100, 200} for Canadian
	 *         currency
	 */
	public int[] getVmCoinKinds() {
		// first we find how many coin kinds there are
		int coinTypes = 0;
		for (int i = 0; i < 100; i++) {
			// when we catch an exception we have ran out of racks, and thus coin types
			try {
				vm.getCoinKindForCoinRack(i);

			} catch (Exception e) {
				break;
			}
			coinTypes++;

		}
		// We use coinTypes to build an array of each coin kind
		int[] coinKinds = new int[coinTypes];
		for (int i = 0; i < coinTypes; i++) {
			coinKinds[i] = vm.getCoinKindForCoinRack(i);
		}
		if (debug) {
			for (int i = 0; i < coinKinds.length; i++) {
				System.out.println(coinKinds[i]);
			}
		}
		return coinKinds;
	}

	/**
	 * a Method to determine if exact change is possible given the prices of the pop
	 * and the current credit Checks if the credit - price can be created using the
	 * available coins is the racks checks for every pop price in the machine.
	 * 
	 * @return possible - A boolean describing if it is possible to create change
	 *         for every possible transaction.
	 */
	public boolean isExactChangePossible() {
		boolean possible = true;
		if (vm.getCoinReturn() != null) {
			for (int i = 0; i < vm.getNumberOfSelectionButtons(); i++) { // get the price for every possible pop
				int credRemaining = credit;
				int price = vm.getPopKindCost(i);
				if (credRemaining >= price) {
					credRemaining -= price;
					int changePossible = 0;

					int[] coinKinds = { 200, 100, 25, 10, 5 }; // legal value of Canadian coins. only types returned
					for (int value = 0; value < coinKinds.length; value++) {
						CoinRack rack = vm.getCoinRackForCoinKind(coinKinds[value]); // the coin rack for the coin value
																						// indicated by the loop
						if (rack != null) { // if rack = null. coin kind is not a valid change option
							int coinsNeeded = 0;
							while ((!rack.isDisabled()) && (credRemaining > changePossible)
									&& (rack.size() > coinsNeeded)) {
								coinsNeeded++;
								changePossible += coinKinds[value]; // sum of available coins
							}
						}
					}
					if (credRemaining != changePossible) // if after going through all the coin racks, the exact change
															// cannot be created
						possible = false; // return that it is not possible to
				}
			}
		} else
			possible = false; // if the CoinReturn is not there (null) return false.

		return possible;
	}

	/**
	 * A method to determine what action should be done when a button is pressed
	 * TODO how is disabling a part going to affect what action is taken?
	 * 
	 * @param button
	 */
	public void determineButtonAction(PushButton button) {
		boolean found = false;

		if (vm.isSafetyEnabled() == false) {
			// search through the selection buttons to see if the parameter button is a
			// selection button
			for (int index = 0; (found == false) && (index < vm.getNumberOfSelectionButtons()); index++) {
				if (vm.getSelectionButton(index) == button) {
					selectionButtonAction(index);
					found = true;
				}
			}
		}
	}

	/**
	 * Method to react to the press of a selection button
	 * 
	 * @param index
	 *            - the index of the selection button that was pressed
	 */
	public void selectionButtonAction(int index) {
		if ((vm.getPopKindCost(index) <= credit) && (circuitEnabled[index] == true)) {
			try {
				vm.getPopCanRack(index).dispensePopCan();
				this.dispensingMessage();
				credit -= vm.getPopKindCost(index); // deduct the price of the pop
				System.out.println("Before CR");
				returnChange(credit); // this doesnt do fuck all
				System.out.println("After CR" + credit);
				if (credit == 0)
					this.welcomeMessageTimer(); // begin cycling the welcome message again
				else
					this.displayCredit();
			} catch (Exception e) {
				System.out.println(e);
			}
		} else if (circuitEnabled[index] != true) {
			setCurrentMessage("Option Unavailable");
			C.DisplayField.setText(getCurrentMessage());
			vm.getDisplay().display("Option unavailable");
		} else {
			this.displayPrice(index);
			// this.displayCredit();
		}
	}

	/**
	 * A method to determine which pop can rack or push button an event has occurred
	 * on needed for EventLog information
	 * 
	 * @param hardware
	 *            - the hardware that the event occurred on
	 * @return The index of the hardware according to the vending machine. -1 means
	 *         error could not find
	 */
	public int findHardwareIndex(AbstractHardware<? extends AbstractHardwareListener> hardware) {
		if (hardware instanceof PopCanRack) {
			for (int index = 0; index < vm.getNumberOfPopCanRacks(); index++) {
				if (vm.getPopCanRack(index) == hardware) {
					return index;
				}
			}
		}

		else if (hardware instanceof PushButton) {
			for (int index = 0; index < vm.getNumberOfSelectionButtons(); index++) {
				if (vm.getSelectionButton(index) == hardware) {
					return index;
				}
			}

			for (int index = 0; index < 37; index++) {
				if (vm.getConfigurationPanel().getButton(index) == hardware) {
					return index;
				}
			}
		}

		else if (hardware instanceof CoinRack)
			for (int i = 0; i < vm.getNumberOfCoinRacks(); i++) {
				if (hardware == vm.getCoinRack(i)) {
					return i;
				}
			}

		return -1; // -1 will be the error index
	}

	/**
	 * Method to disable a piece of hardware. If hardware is a selection button or
	 * pop rack, machine can remain operational, otherwise, disable vending machine
	 * 
	 * @param hardware
	 */
	public void disableHardware(AbstractHardware<? extends AbstractHardwareListener> hardware) {
		if (hardware instanceof PopCanRack) {
			circuitEnabled[findHardwareIndex(hardware)] = false;
		} else if (hardware instanceof PushButton) {
			for (int i = 0; i < vm.getNumberOfSelectionButtons(); i++) {
				if (hardware == vm.getSelectionButton(i)) {
					circuitEnabled[i] = false;
				}
			}
		} else {
			C.Card.setText("OOO Light On");
			vm.getOutOfOrderLight().activate();
			try {
				returnChange(credit);
			} catch (Exception e) {
				System.out.println(e);
			}
			vendOutOfOrder();
			// vm.enableSafety(); NOTE: calling enableSafety() will result in a stack
			// overflow exception
		}
	}

	/**
	 * Method to disable a piece of hardware. If hardware is a selection button or
	 * pop rack, machine can remain operational, otherwise, disable vending machine
	 * 
	 * @param hardware
	 */
	public void enableHardware(AbstractHardware<? extends AbstractHardwareListener> hardware) {
		if (hardware instanceof PopCanRack) {
			int index = findHardwareIndex(hardware);
			if ((vm.getSelectionButton(index).isDisabled() == false) && (vm.isSafetyEnabled() == false))
				circuitEnabled[index] = true;
		} else if (hardware instanceof PushButton) {
			for (int i = 0; i < vm.getNumberOfSelectionButtons(); i++) {
				if (hardware == vm.getSelectionButton(i)) {
					circuitEnabled[i] = true;
				}
			}
		} else {
			C.Card.setText("ECO Light Off");
			vm.getOutOfOrderLight().deactivate();
			// vm.disableSafety(); NOTE: This may result in a stack overflow exception

		}
	}

	/**
	 * Method returns the value in the circuitEnabled array at an index
	 * 
	 * @param int
	 *            index, the index of the desired value
	 * @retupublic void bitcoinTransaction() {
	 * 
	 * 
	 * 
	 *             }rn boolean circuitEnabled[index]
	 */
	public boolean getCircuitEnabledIndex(int index) {
		if (index < 0 || index >= circuitEnabled.length) {
			throw new IndexOutOfBoundsException();
		} else
			return circuitEnabled[index];
	}

}
