package com.example.tugasmobile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    
    private lateinit var transactions: List<Transaction>

   
    private lateinit var transactionAdapter: TransactionAdapter

 
    private lateinit var linearLayoutManager: LinearLayoutManager

    
    private lateinit var db: AppDatabase

   
    private lateinit var deletedTransaction: Transaction

  
    private lateinit var oldTransactions: List<Transaction>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

   
        transactions = arrayListOf()

        transactionAdapter = TransactionAdapter(transactions)

   
        linearLayoutManager = LinearLayoutManager(this)

     
        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "transactions"
        ).build()

    
        val recycleView = findViewById<RecyclerView>(R.id.recycle_view)

       
        val addBtn = findViewById<FloatingActionButton>(R.id.addBtn)

       
        recycleView.adapter = transactionAdapter

        
        recycleView.layoutManager = linearLayoutManager

        
        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.RIGHT
        ) {

          
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

           
            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {

                
                val position = viewHolder.adapterPosition

             
                deleteTransaction(transactions[position])
            }
        }

    
        val swipeHelper = ItemTouchHelper(itemTouchHelper)
        swipeHelper.attachToRecyclerView(recycleView)

       
        addBtn.setOnClickListener {

            
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)

  
    private fun fetchAll() {

        GlobalScope.launch {

           
            transactions = db.transactionDao().getAll()

            runOnUiThread {

               
                updateDashboard()

                
                transactionAdapter.setData(transactions)
            }
        }
    }

    
    private fun updateDashboard() {

        val balance = findViewById<TextView>(R.id.Balance)
        val budget = findViewById<TextView>(R.id.budget)
        val expense = findViewById<TextView>(R.id.expenses)

      
        val totalAmount = transactions.map { it.amount }.sum()

      
        val budgetAmount = transactions
            .filter { it.amount > 0 }
            .map { it.amount }
            .sum()

       
        val expenseAmount = totalAmount - budgetAmount

      
        balance.text = "Rp.%.0f".format(totalAmount)

   
        budget.text = "Rp.%.0f".format(budgetAmount)

      
        expense.text = "Rp.%.0f".format(expenseAmount)
    }

    @OptIn(DelicateCoroutinesApi::class)

  
    private fun deleteTransaction(transaction: Transaction) {

       
        deletedTransaction = transaction

   
        oldTransactions = transactions

        GlobalScope.launch {

            
            db.transactionDao().delete(
                listOf(transaction)
            )

           
            transactions = transactions.filter {
                it.id != transaction.id
            }

            runOnUiThread {

            
                updateDashboard()

            
                transactionAdapter.setData(transactions)

                showSnackBar()
            }
        }
    }

    
    private fun showSnackBar() {

        
        val view = findViewById<android.view.View>(R.id.main)

        
        val snackbar = Snackbar.make(
            view,
            "Transaction Deleted",
            Snackbar.LENGTH_LONG
        )

        
        snackbar.setAction("Undo") {

           
            undoDelete()
        }

       
        snackbar.setActionTextColor(
            ContextCompat.getColor(this, R.color.red)
        )

      
        snackbar.show()
    }

    @OptIn(DelicateCoroutinesApi::class)

    
    private fun undoDelete() {

        GlobalScope.launch {

            
            db.transactionDao().insertAll(
                listOf(deletedTransaction)
            )

            
            transactions = oldTransactions

            runOnUiThread {

                
                updateDashboard()

                
                transactionAdapter.setData(transactions)
            }
        }
    }

   
    override fun onResume() {
        super.onResume()

        
        fetchAll()
    }
}
