package com.gmail.haloinverse.DynamicMarket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

//import com.bukkit.haloinverse.SimpleMarket.DatabaseCore.Type;

		  
		  // NOTE:
		  // For fluctuating market, current_price = base_price * (1+stepSize)^(-stockLevel/volatilityFactor)
		  // volatilityFactor: Speed of market fluctuations. volatility=64 changes price 64 times slower than volatility=1.
		  
		  // Future DB structure:
		  // id: Unique auto-increment key.
		  // item: Item ID
		  // subtype: Item subtype
		  // name: Name of item+subtype
		  // basePrice: Base price of item at stock=0.
		  // stock: Current stock of item.
		  // volatility: % change in price per 1 stock bought/sold
		  // salesTax: Ratio of buy to sell prices. sellPrice = buyPrice * (1 - (salesTax/100)).
		  // stockMin: Minimum stock beyond which purchases will fail.
		  // stockMax: Maximum stock beyond which sales will fail.
		  // stockFloor: Lower cap on stock count.
		  // stockCeil: Upper cap on stock count.
		  // minPrice: Minimum price of items.
		  // maxPrice: Maximum price of items.
		  
		  //CHANGED: Many redundant methods removed.
		  //Old functionality can be duplicated by getting the fields of the returned ShopItem object.
		  
 public class DatabaseMarket extends DatabaseCore
 {
 	//public Type database = null;
 	public Items itemsReference = null;

	public DatabaseMarket(Type database, String tableAccessed, Items itemsRef, String thisEngine, DynamicMarket pluginRef)
	{
		super(database, tableAccessed, thisEngine, pluginRef);  // default table name: "Market"
		this.itemsReference = itemsRef;
	}
	
	@Deprecated
	public DatabaseMarket(Type database, String tableAccessed, String thisEngine, DynamicMarket pluginRef)
	{
		super(database, tableAccessed, thisEngine, pluginRef);
		this.itemsReference = null;
	}

	@Deprecated
	protected boolean createTable()
	{
		return createTable("");
	}
	
	protected boolean createTable(String shopLabel) {
		SQLHandler myQuery = new SQLHandler(this);
		if (this.database.equals(Type.SQLITE))
			myQuery.executeStatement("CREATE TABLE " + tableName+shopLabel + " ( id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"item INT NOT NULL, " +
					"subtype INT NOT NULL, " +
					"name TEXT NOT NULL, " +
					"count INT NOT NULL, " +
					"baseprice INT NOT NULL, " +
					"canbuy INT NOT NULL, " +
					"cansell INT NOT NULL, " +
					"stock INT NOT NULL, " +
					"volatility INT NOT NULL, " +
					"salestax INT NOT NULL, " +
					"stockhighest INT NOT NULL, " +
					"stocklowest INT NOT NULL, " +
					"stockfloor INT NOT NULL, " +
					"stockceil INT NOT NULL, " +
					"pricefloor INT NOT NULL, " +
					"priceceil INT NOT NULL, " +
					"jitterperc INT NOT NULL, " +
					"driftout INT NOT NULL, " +
					"driftin INT NOT NULL, " +
					"avgstock INT NOT NULL, " +
					"class INT NOT NULL);" +
					"CREATE INDEX itemIndex ON Market (item);" +
					"CREATE INDEX subtypeIndex on Market (subtype);" +
					"CREATE INDEX nameIndex on Market (name)");
		else
			myQuery.executeStatement("CREATE TABLE " + tableName+shopLabel + " ( id INT( 255 ) NOT NULL AUTO_INCREMENT, " +
					"item INT NOT NULL, " +
					"subtype INT NOT NULL, " +
					"name CHAR(20) NOT NULL, " +
					"count INT NOT NULL, " +
					"baseprice INT NOT NULL, " +
					"stock INT NOT NULL, " +
					"canbuy INT NOT NULL, " +
					"cansell INT NOT NULL, " +
					"volatility INT NOT NULL, " +
					"salestax INT NOT NULL, " +
					"stocklowest INT NOT NULL, " +
					"stockhighest INT NOT NULL, " +
					"stockfloor INT NOT NULL, " +
					"stockceil INT NOT NULL, " +
					"pricefloor INT NOT NULL, " +
					"priceceil INT NOT NULL, " +
					"jitterperc INT NOT NULL, " +
					"driftout INT NOT NULL, " +
					"driftin INT NOT NULL, " +
					"avgstock INT NOT NULL, " +
					"class INT NOT NULL, " +
					"PRIMARY KEY ( id ), INDEX ( item, subtype, name )) ENGINE = "+ engine + ";");
		myQuery.close();
		
		if (!myQuery.isOK)
			return false;
		
		// Add default record.
		
		return add(new MarketItem("-1,-1 n:Default", null, this, shopLabel), shopLabel);
	}
			
	@Deprecated
	public boolean add(Object addRef)
	{
		if (addRef instanceof MarketItem)
			return add((MarketItem)addRef, "");
		return false;
	}
	
	public boolean add(MarketItem newItem, String shopLabel) {
	  //TODO: Check for pre-existing records.

		SQLHandler myQuery = new SQLHandler(this);

		myQuery.inputList.add(newItem.itemId);
		myQuery.inputList.add(newItem.subType);
		myQuery.inputList.add(newItem.count);
		myQuery.inputList.add(newItem.getName());
		myQuery.inputList.add(newItem.basePrice);
		myQuery.inputList.add(newItem.stock);
		myQuery.inputList.add(newItem.canBuy? 1 : 0);
		myQuery.inputList.add(newItem.canSell? 1 : 0);
		myQuery.inputList.add(newItem.getVolatility());
		myQuery.inputList.add(newItem.salesTax);
		myQuery.inputList.add(newItem.stockLowest);
		myQuery.inputList.add(newItem.stockHighest);
		myQuery.inputList.add(newItem.stockFloor);
		myQuery.inputList.add(newItem.stockCeil);
		myQuery.inputList.add(newItem.priceFloor);
		myQuery.inputList.add(newItem.priceCeil);
		
		myQuery.prepareStatement("INSERT INTO " + tableName+shopLabel + " (item, subtype, count, name, baseprice, stock, canbuy, cansell, volatility, " +
		"salestax, stocklowest, stockhighest, stockfloor, stockceil, pricefloor, priceceil, class, jitterperc, driftin, driftout, avgstock) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0,0,0)");
		
		myQuery.executeUpdate();
		
		myQuery.close();
		return (myQuery.isOK);
	}

	@Deprecated
	public boolean update(MarketItem updated)
	{
		return update(updated, "");
	}
	
	public boolean update(MarketItem updated, String shopLabel) {
		SQLHandler myQuery = new SQLHandler(this);

		myQuery.inputList.add(updated.count);
		myQuery.inputList.add(updated.getName());
		myQuery.inputList.add(updated.basePrice);
		myQuery.inputList.add(updated.stock);
		myQuery.inputList.add(updated.canBuy? 1 : 0);
		myQuery.inputList.add(updated.canSell? 1 : 0);
		myQuery.inputList.add(updated.getVolatility());
		myQuery.inputList.add(updated.salesTax);
		myQuery.inputList.add(updated.stockLowest);
		myQuery.inputList.add(updated.stockHighest);
		myQuery.inputList.add(updated.stockFloor);
		myQuery.inputList.add(updated.stockCeil);
		myQuery.inputList.add(updated.priceFloor);
		myQuery.inputList.add(updated.priceCeil);

		myQuery.inputList.add(updated.itemId);
		myQuery.inputList.add(updated.subType);
		
		myQuery.prepareStatement("UPDATE " + tableName+shopLabel + " SET count = ?, name = ?, baseprice = ?, stock = ?, canbuy = ?, cansell = ?, volatility = ?, " +
				"salestax = ?, stocklowest = ?, stockhighest = ?, stockfloor = ?, stockceil = ?, pricefloor = ?, priceceil = ? WHERE item = ? AND subtype = ?" + ((this.database.equals(Type.SQLITE)) ? "" : " LIMIT 1"));

		myQuery.executeUpdate();
		
		myQuery.close();
		return (myQuery.isOK);
	}
	
	@Deprecated
	public boolean update(Object updateRef)
	{
		if (updateRef instanceof MarketItem)
			return update((MarketItem)updateRef);
		return false;
	}
	
	@Deprecated
	public ArrayList<MarketItem> list(int pageNum)
	{
		return list(pageNum, null, "");
	}
	
	public ArrayList<MarketItem> list(int pageNum, String nameFilter, String shopLabel) {
		//CHANGED: This now spits out a list of MarketItems, instead of a list of arrays of ints.
	  //If pageNum=0, return the entire list.
		// Optionally filters by partial-string matching of names.

	  //TODO: Add option for name-sorting.
		SQLHandler myQuery = new SQLHandler(this);
		ArrayList<MarketItem> data = new ArrayList<MarketItem>();
	  	int startItem = 0;
	  	int numItems = 9999999;
	  	if (pageNum > 0)
	  	{
	  		startItem = (pageNum - 1) * 8;
	  		numItems = 8;
	  	}
	  
	  	if ((nameFilter == null) || (nameFilter.isEmpty()))
	  	{
	  		myQuery.inputList.add(startItem);
	  		myQuery.inputList.add(numItems);
	  		myQuery.prepareStatement("SELECT * FROM " + tableName+shopLabel + " WHERE item >= 0 ORDER BY item ASC, subtype ASC LIMIT ?, ?");
	  	}
	  	else
	  	{
	  		myQuery.inputList.add("%" + nameFilter + "%");
	  		myQuery.inputList.add(startItem);
	  		myQuery.inputList.add(numItems);
	  		myQuery.prepareStatement("SELECT * FROM " + tableName+shopLabel + " WHERE (item >= 0 AND name LIKE ?) ORDER BY item ASC, subtype ASC LIMIT ?, ?");
	  	}	  	
		
		myQuery.executeQuery();
		
		if (myQuery.isOK)
			try {
				while (myQuery.rs.next())
					//data.add(new ShopItem(myQuery.rs.getInt("item"),myQuery.rs.getInt("type"), myQuery.rs.getInt("buy"), myQuery.rs.getInt("sell"), myQuery.rs.getInt("per") ));
					data.add(new MarketItem(myQuery, shopLabel));
			} catch (SQLException ex) {
				logSevereException("SQL Error during ArrayList fetch: " + dbTypeString(), ex);
			}
		
		myQuery.close();

		return data;
	}
	
	@Deprecated
	public MarketItem data(ItemClump thisItem)
	{
		return data(thisItem, "");
	}
	
	public MarketItem data(ItemClump thisItem, String shopLabel) {
	  //CHANGED: Returns MarketItems now.
		SQLHandler myQuery = new SQLHandler(this);
		MarketItem data = null;

		myQuery.inputList.add(thisItem.itemId);
		myQuery.inputList.add(thisItem.subType);
		myQuery.prepareStatement("SELECT * FROM " + tableName+shopLabel + " WHERE item = ? AND subtype = ? LIMIT 1");

		myQuery.executeQuery();

		try {
			if (myQuery.rs != null)
				if (myQuery.rs.next())
					//data = new ShopItem(myQuery.rs.getInt("item"), myQuery.rs.getInt("type"), myQuery.rs.getInt("buy"), myQuery.rs.getInt("sell"), myQuery.rs.getInt("per"));
					data = new MarketItem(myQuery, shopLabel);
					// TODO: Change constructor to take a ResultSet and throw SQLExceptions.
		} catch (SQLException ex) {
			logSevereException("Error retrieving shop item data with " + dbTypeString(), ex);
			data = null;
		}
		
		// Temp until constructor throws SQLExceptions
		if (!(myQuery.isOK))
			data = null;
		
		myQuery.close();

		//if (data == null) { data = new MarketItem(); }
		//Return null if no matching data found.
		
		return data;
	}
	
	public boolean hasRecord(ItemClump thisItem, String shopLabel)
	{
		// Checks if a given ItemClump has a database entry.
		SQLHandler myQuery = new SQLHandler(this);
		boolean returnVal = false;

		myQuery.inputList.add(thisItem.itemId);
		myQuery.inputList.add(thisItem.subType);
		myQuery.prepareStatement("SELECT * FROM " + tableName+shopLabel + " WHERE item = ? AND subtype = ? LIMIT 1");

		myQuery.executeQuery();
		
		try {
			if (myQuery.rs != null)
				if (myQuery.rs.next())
					returnVal = true;
		} catch (SQLException ex) {
			logSevereException("Error retrieving shop item data with " + dbTypeString(), ex);
		}
		
		return returnVal;
	}
		
	public boolean addStock(ItemClump thisItem, String shopLabel)
	{
		SQLHandler myQuery = new SQLHandler(this);

		myQuery.inputList.add(thisItem.count);
		myQuery.inputList.add(thisItem.itemId);
		myQuery.inputList.add(thisItem.subType);
		if (this.database.equals(Type.SQLITE))
			myQuery.prepareStatement("UPDATE " + tableName+shopLabel + " SET stock = min(stock + ?, stockceil) WHERE item = ? AND subtype = ? " + ((this.database.equals(Type.SQLITE)) ? "" : " LIMIT 1"));
		else
			myQuery.prepareStatement("UPDATE " + tableName+shopLabel + " SET stock = LEAST(stock + ?, stockceil) WHERE item = ? AND subtype = ? " + ((this.database.equals(Type.SQLITE)) ? "" : " LIMIT 1"));
			

		myQuery.executeUpdate();
		
		myQuery.close();

		return myQuery.isOK;
	}
	
	public boolean removeStock(ItemClump thisItem, String shopLabel)
	{
		SQLHandler myQuery = new SQLHandler(this);

		myQuery.inputList.add(thisItem.count);
		myQuery.inputList.add(thisItem.itemId);
		myQuery.inputList.add(thisItem.subType);
		if (this.database.equals(Type.SQLITE))
			myQuery.prepareStatement("UPDATE " + tableName+shopLabel + " SET stock = max(stock - ?, stockfloor) WHERE item = ? AND subtype = ? " + ((this.database.equals(Type.SQLITE)) ? "" : " LIMIT 1"));
		else
			myQuery.prepareStatement("UPDATE " + tableName+shopLabel + " SET stock = GREATEST(stock - ?, stockfloor) WHERE item = ? AND subtype = ? " + ((this.database.equals(Type.SQLITE)) ? "" : " LIMIT 1"));
		
		myQuery.executeUpdate();
		
		myQuery.close();

		return myQuery.isOK;
	}
	
	public MarketItem getDefault(String shopLabel)
	{
		return data(new ItemClump(-1, -1), shopLabel);
	}
	
	public ItemClump nameLookup(String searchName, String shopLabel)
	{
		// Parses a name string into an ItemClump with referenced itemId, subtype
		// If name lookup fails, returns null
		// Tries market database names, and if it fails, tries itemsReference.
		ItemClump returnData = null;
		
		if (searchName.equalsIgnoreCase("default"))
			return (new ItemClump(-1,-1));
		
		SQLHandler myQuery = new SQLHandler(this);

		myQuery.inputList.add(searchName);
		myQuery.prepareStatement("SELECT item, subtype FROM " + tableName+shopLabel + " WHERE name = ? LIMIT 1");

		myQuery.executeQuery();

		try
		{
			if (myQuery.rs != null)
				if (myQuery.rs.next())
					returnData = new ItemClump(myQuery.rs.getInt("item"), myQuery.rs.getInt("subtype"));
		} catch (SQLException ex) {
			logSevereException("Error retrieving item name with " + dbTypeString(), ex);
			returnData = null;
		}
		
		// Temp until constructor throws SQLExceptions
		if (!(myQuery.isOK))
			returnData = null;
		
		myQuery.close();
		
		if (returnData != null)
			return returnData;
		
		// If straight lookup failed, try partial lookup.
		myQuery = new SQLHandler(this);

		myQuery.inputList.add("%" + searchName + "%");
		myQuery.prepareStatement("SELECT item, subtype FROM " + tableName+shopLabel + " WHERE name LIKE ? LIMIT 1");

		myQuery.executeQuery();

		try
		{
			if (myQuery.rs != null)
				if (myQuery.rs.next())
					returnData = new ItemClump(myQuery.rs.getInt("item"), myQuery.rs.getInt("subtype"));
		} catch (SQLException ex) {
			logSevereException("Error retrieving item name with " + dbTypeString(), ex);
			returnData = null;
		}
		
		// Temp until constructor throws SQLExceptions
		if (!(myQuery.isOK))
			returnData = null;
		
		myQuery.close();
		
		// On lookup failure, try the Items flatfile.
		if (returnData == null)
			if (itemsReference != null)
				returnData = itemsReference.nameLookup(searchName);

		return returnData;
	}
	
	public String getName(ItemClump itemData, String shopLabel)
	{
		String returnedName = null;
		SQLHandler myQuery = new SQLHandler(this);
		
		myQuery.inputList.add(itemData.itemId);
		myQuery.inputList.add(itemData.subType);
		
		myQuery.prepareStatement("SELECT name FROM " + tableName+shopLabel + " WHERE item = ? AND subtype = ? LIMIT 1");
		myQuery.executeQuery();

		try
		{
			if (myQuery.rs != null)
				if (myQuery.rs.next())
					returnedName = myQuery.getString("name");
		} catch (SQLException ex) {
			logSevereException("Error retrieving item name with " + dbTypeString(), ex);
			returnedName = null;
		}
		
		myQuery.close();
		
		if (returnedName == null)
			returnedName = itemsReference.name(itemData);
		
		if ((returnedName == null) && (itemData.isDefault()))
			returnedName = "DEFAULT";
		
		if (returnedName == null)
			return "UNKNOWN";
		
		return returnedName;
		
	}
	
	@Deprecated
	public boolean remove(ItemClump removed)
	{
		return remove(removed, "");
	}
	
	public boolean remove(ItemClump removed, String shopLabel) {
		  // CHANGED: Now accepts an itemType (through use of the ItemClump class). 
			SQLHandler myQuery = new SQLHandler(this);

			myQuery.inputList.add(removed.itemId);
			myQuery.inputList.add(removed.subType);
			myQuery.prepareStatement("DELETE FROM " + tableName+shopLabel + " WHERE item = ? AND subtype = ?" + ((this.database.equals(Type.SQLITE)) ? "" : " LIMIT 1"));

			myQuery.executeUpdate();
			
			myQuery.close();
			return myQuery.isOK;
		}

	public boolean dumpToCSV(String fileName, String shopLabel)
	{
		// Dumps this database into a .csv file.
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(fileName, false));
		} catch (IOException ex) {
			logSevereException("Error opening .csv for writing: " + fileName, ex);
			return false;
		}

		String line;
		
		line = MarketItem.csvHeaderLine(); 
		try {
			writer.write(line);
		} catch (IOException ex) {
			logSevereException("I/O error writing .csv header:" + fileName, ex);
			return false;
		}
		
		ArrayList<MarketItem> itemsToWrite = list(0, null, shopLabel);
		
		for (MarketItem thisItem : itemsToWrite)
		{
			// Write a line.
			line = thisItem.csvLine();
			try {
				writer.newLine();
				writer.write(line);
			} catch (IOException ex) {
				logSevereException("Error writing output line to .csv: " + fileName, ex);
				return false;
			}
		}
		try {
			writer.flush();
		} catch (IOException ex) {
			logSevereException("Error flushing output after writing .csv:" + fileName, ex);
		}
		try {
			writer.close();
		} catch (IOException ex) {
			logSevereException("Error closing file after writing .csv:" + fileName, ex);
		}
		return true;
	}
	
	public boolean inhaleFromCSV(String fileName, String shopLabel)
	{
		// Sucks a .csv file into this database.
		BufferedReader reader;
		MarketItem importItem;
		try {
			reader = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException ex) {
			logSevereException("File not found while importing .csv: " + fileName, ex);
			return false;
		}

		String line;
		try {
			line = reader.readLine();
			//if (!line.equalsIgnoreCase(MarketItem.csvHeaderLine()))
			//{
			//	plugin.log.severe("[" + plugin.name + "]: Bad header line reading .csv: " + fileName);
			//	return false;
			//}
			line = reader.readLine();
		} catch (IOException ex) {
			logSevereException("I/O error importing .csv:" + fileName, ex);
			return false;
		}
		while (line != null)
		{
			if (line.trim().length() == 0)
			{
				continue;
			}
			// Parse a line.
			//plugin.log.info(line);
			line = line.replace("'","").replace("\"","");
			importItem = new MarketItem(line, null, true);
			if (hasRecord(importItem, shopLabel))
			{
				importItem = new MarketItem(line, data(importItem, shopLabel), true);
				update(importItem, shopLabel);
			}
			else
			{
				add(importItem, shopLabel);
			}
			try {
				line = reader.readLine();
			} catch (IOException ex) {
				logSevereException("I/O error reading .csv:" + fileName, ex);
				break;
			}
		}
		try {
			reader.close();
		} catch (IOException ex) {
			logSevereException("Error closing file after importing .csv: " + fileName, ex);
		}
		return true;
	}

}