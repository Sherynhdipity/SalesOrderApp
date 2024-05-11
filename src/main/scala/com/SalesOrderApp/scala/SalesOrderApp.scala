package com.SalesOrderApp.scala

import java.sql.{Connection, DriverManager, ResultSet}
import java.text.{DecimalFormat, SimpleDateFormat}
import javax.swing.BorderFactory
import javax.swing.table.DefaultTableModel
import scala.collection.mutable.ArrayBuffer
import scala.swing._
import scala.swing.event.{SelectionChanged, TableRowsSelected}

object SalesOrderApp extends SimpleSwingApplication {
  // Database connection details
  val url = "jdbc:mysql://localhost:3306/northwind"
  val username = "root"
  val password = ""
  val driver = "com.mysql.cj.jdbc.Driver"
  var connection: Connection = _

  // Establish database connection
  try {
    Class.forName(driver)
    connection = DriverManager.getConnection(url, username, password)
  } catch {
    case e: Exception => e.printStackTrace
  }

  // Get data from database to populate ComboBox with customer IDs
  def populateCustomerIDs: Seq[String] = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery("SELECT custId FROM customer ORDER BY custId")
    val customerIDs = ArrayBuffer[String]()
    while (resultSet.next()) {
      customerIDs += resultSet.getString("custId")
    }
    customerIDs.toSeq
  }

  // Get customer details for a specific customer ID
  def getCustomerDetails(customerId: String): (String, String, String) = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(s"SELECT contactName, companyName, city FROM customer WHERE custId = '$customerId'")
    if (resultSet.next()) {
      (
        resultSet.getString("contactName"),
        resultSet.getString("companyName"),
        resultSet.getString("city")
      )
    } else {
      ("", "", "")
    }
  }

  // Get data from database to populate order table based on selected customer ID
  def populateOrderTable(customerId: String, connection: Connection): (Seq[Array[Any]], Int) = {
    // Query the database
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(
    s"SELECT so.orderId AS oid, so.orderDate AS od, so.shipCountry AS sco, so.shipCity AS sci " +
    s"FROM salesOrder so JOIN customer c ON so.custId = c.custId WHERE c.custId = '$customerId'"
    )

    // Create a buffer for storing order data
    val tableData = ArrayBuffer[Array[Any]]()
    var orderCount = 0

    // Process the result set and collect data
    while (resultSet.next()) {
    orderCount += 1 // Increment the order count
    val orderId = resultSet.getString("oid")
    val orderDate = resultSet.getDate("od")
    val shipCountry = resultSet.getString("sco")
    val shipCity = resultSet.getString("sci")
    val formattedOrderDate = new SimpleDateFormat("MM/dd/yyyy").format(orderDate)

    // Collect row data
    val rowData = Array[Any](orderId, formattedOrderDate, shipCountry, shipCity)
    tableData += rowData
  }

    // Return the tuple: sequence of order data and the order count
    (tableData.toSeq, orderCount)
  }


  // Function to populate product details table and calculate the additional columns
  def populateProductDetailsTable(orderId: String): (Seq[Array[Any]], Int) = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(
    s"SELECT p.productId AS pid, od.unitPrice AS up, od.quantity AS q, od.discount AS d FROM orderdetail od JOIN product p ON p.productId = od.productId WHERE od.orderId = '$orderId'"
    )

    val tableData = ArrayBuffer[Array[Any]]()
    var productCount = 0

    // Initialize a DecimalFormat object to format values
    val decimalFormat = new DecimalFormat("P #.##")

    while (resultSet.next()) {
    productCount += 1

    // Retrieve values from the database
    val productId = resultSet.getString("pid")
    val unitPrice = resultSet.getDouble("up")
    val quantity = resultSet.getInt("q")
    val percentDiscount = resultSet.getDouble("d")

    // Calculate additional values
    val amount = unitPrice * quantity
    val totalDiscount = (percentDiscount / 100) * amount
    val discountedValue = amount - totalDiscount

    // Format the values
    val formattedUnitPrice = decimalFormat.format(unitPrice)
    val formattedAmount = decimalFormat.format(amount)
    val formattedTotalDiscount = decimalFormat.format(totalDiscount)
    val formattedDiscountedValue = decimalFormat.format(discountedValue)

    // Create a row with all the columns
    val rowData = Array[Any](
    productId,
    formattedUnitPrice,
    quantity,
    percentDiscount,
    formattedAmount,
    formattedTotalDiscount,
    formattedDiscountedValue
    )

    // Add row data to table data
    tableData += rowData
  }

    // Return the data and the product count as a tuple
    (tableData, productCount)
  }


  // Swing UI setup
  def top: MainFrame = new MainFrame {
    title = "Sales Order App - Scala"

    // Initialize Swing components
    val customertxtFields = List(
      new TextField { columns = 10; enabled = false }, // Contact Name
      new TextField { columns = 15; enabled = false }, // Company Name
      new TextField { columns = 10; enabled = false } // City
    )

    val valuetxtFields = List(
      new TextField { columns = 10; enabled = false ; foreground = java.awt.Color.BLACK }, // Total Amount
      new TextField { columns = 10; enabled = false; foreground = java.awt.Color.BLACK }, // Total Discount
      new TextField { columns = 10; enabled = false; foreground = java.awt.Color.BLACK } // Total Discounted Value
    )

    val customerLabels = List(
      new Label("Customer Name: "),
      new Label("Company Name: "),
      new Label("City: "),
      new Label("  "),
      new Label("Customer ID: ")
    )

    val cmbCustomerId = new ComboBox(populateCustomerIDs)

    val orderTableModel = new DefaultTableModel(0, 4) {
      override def getColumnName(column: Int): String = column match {
        case 0 => "Order Id"
        case 1 => "Order Date"
        case 2 => "Ship Country"
        case 3 => "Ship City"
        case _ => ""
      }
      override def isCellEditable(row: Int, column: Int): Boolean = false
    }

    val orderTable = new Table {
      model = orderTableModel
    }

    val productDetailsTableModel = new DefaultTableModel(0, 7) {
      override def getColumnName(column: Int): String = column match {
        case 0 => "Product Id"
        case 1 => "Unit Price"
        case 2 => "Quantity"
        case 3 => "% Discount"
        case 4 => "Amount"
        case 5 => "Total Discount"
        case 6 => "Discounted Value"
        case _ => ""
      }
      override def isCellEditable(row: Int, column: Int): Boolean = false
    }

    val productDetailsTable = new Table {
      model = productDetailsTableModel
    }

    val orderCountLabel = new Label("")
    val productCountLabel = new Label("")

    // LeftInnerPanel: holds labels, text fields, and combo box
    val leftInnerPanel = new GridPanel(4, 2) {
      hGap = 10
      vGap = 10
      contents ++= customerLabels.zip(customertxtFields).flatMap { case (label, textField) => Seq(label, textField) }
      contents += new Label("Customer ID: ")
      contents += cmbCustomerId
      // Add padding around the panel
      border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    }

    // Left panel: holds LeftInnerPanel and orderScrollPane
    val orderScrollPane = new ScrollPane(orderTable)
    val leftPanel = new GridPanel(3, 1) {
      vGap = 10
      contents += leftInnerPanel
      contents += orderScrollPane
      contents += orderCountLabel
      // Add padding around the panel
      border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    }

    // Right panel: holds prodDeetScrollPane, valuetxtFields, and productCountLabel
    val prodDeetScrollPane = new ScrollPane(productDetailsTable)
    val flowPanel = new FlowPanel(FlowPanel.Alignment.Left)(
      valuetxtFields(0), valuetxtFields(1), valuetxtFields(2)
    )
    val rightPanel = new GridPanel(3, 1) {
      vGap = 10
      contents += prodDeetScrollPane
      contents += flowPanel
      contents += productCountLabel
      // Add padding around the panel
      border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    }

    // Main panel: holds leftPanel and rightPanel
    val mainPanel = new GridPanel(1, 2) {
      hGap = 20
      vGap = 20
      contents += leftPanel
      contents += rightPanel
      // Add padding around the panel
      border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
    }

    contents = mainPanel

    // Event listeners
    listenTo(cmbCustomerId.selection)
    listenTo(orderTable.selection)

    reactions += {
      // Update UI based on selected customer ID
      case SelectionChanged(`cmbCustomerId`) =>
        val selectedCustomerId = cmbCustomerId.selection.item
        val (contactName, companyName, city) = getCustomerDetails(selectedCustomerId)
        customertxtFields(0).text = contactName
        customertxtFields(1).text = companyName
        customertxtFields(2).text = city

        // Assuming populateOrderTable returns a tuple (Seq[Array[Any]], Int)
        val (orderData, orderCount) = populateOrderTable(selectedCustomerId, connection)

        // You can use orderData as Seq[Array[Any]] and orderCount as Int
        orderTableModel.setRowCount(0) // Clear existing rows
        orderData.foreach(row => orderTableModel.addRow(row.asInstanceOf[Array[Object]]))
        orderCountLabel.text = s"There are $orderCount sales order records for $selectedCustomerId"

        // Update product details based on the selected order
        reactions += {
          case TableRowsSelected(_, _, _) =>
            val selectedRow = orderTable.selection.rows.headOption.getOrElse(-1)
            if (selectedRow != -1) {
              val orderId = orderTable.model.getValueAt(selectedRow, 0).toString
              val (productDetailsData, productCount) = populateProductDetailsTable(orderId)
              productDetailsTableModel.setRowCount(0) // Clear existing rows

              // Populate the table model with product details and calculated columns
              productDetailsData.foreach(row => productDetailsTableModel.addRow(row.asInstanceOf[Array[Object]]))

              // Update the label for product count
              productCountLabel.text = s"There are $productCount products for order number $orderId"

              // Calculate sums for the Amount, Total Discount, and Discounted Value columns
              val amountSum = productDetailsData.map(row => row(4).toString.stripPrefix("P ").toDouble).sum
              val totalDiscountSum = productDetailsData.map(row => row(5).toString.stripPrefix("P ").toDouble).sum
              val discountedValueSum = productDetailsData.map(row => row(6).toString.stripPrefix("P ").toDouble).sum

              // Format the sums with the peso sign and two decimal places
              val decimalFormat = new DecimalFormat("P #.00")

              // Update the text fields with the calculated sums
              valuetxtFields(0).text = decimalFormat.format(amountSum)
              valuetxtFields(1).text = decimalFormat.format(totalDiscountSum)
              valuetxtFields(2).text = decimalFormat.format(discountedValueSum)
            }
        }
    }
  }
}
