package com.santos.kotlin.coroutinetest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Button
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class MainActivity : AppCompatActivity(), CoroutineScope {
    val job = SupervisorJob()
    val channel = Channel<View>(Channel.CONFLATED)
    val channel2 = Channel<View>(Channel.CONFLATED)
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main.immediate + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val view1 = findViewById<Button>(R.id.Confirm)
        val view2 = findViewById<Button>(R.id.Click1)

        view1.setOnClickListener {
            Log.d("coroutinetestabcd", "我是button view click at ${Thread.currentThread().name}")
            channel.offer(it)
        }

        view2.setOnClickListener {
            Log.d("coroutinetestabcd", "我是button view2 click at ${Thread.currentThread().name}")
            channel2.offer(it)
        }

        test1()

        launch {
            for(view in channel2){
                processMyClickJobSequence()
            }
        }

    }

    suspend fun processMyClickJobSequence() {
        Log.d("coroutinetestabcd", "我是processMyClickJobSequence at ${Thread.currentThread().name}")
        delay(1000)
        Log.d("coroutinetestabcd", "我是processMyClickJobSequence done at ${Thread.currentThread().name}")
    }

    private fun test1() {
        launch(coroutineContext + CoroutineName("我是工作一")) {
            我是工作一()
            Log.d("coroutinetestabcd", "我是工作一 done at ${Thread.currentThread().name}, jos is ${coroutineContext[CoroutineName]}")
        }
        launch {
            我是工作二()
            Log.d("coroutinetestabcd", "我是工作二 done at ${Thread.currentThread().name}")
        }

        launch {
            showLoading()
            //IO load bitmap
            val bitmap = 我是工作三().await()
            //UI updat bitmap
            updateResult()

            val re = suspendContFun<String>(Dispatchers.IO){context, cont ->
                Log.d("coroutinetestabcd", "我是工作三 suspendContFun at ${Thread.currentThread().name}")
                cont.resume("我是工作三 suspendContFun Result")
            }.await()

            updateResult(re)
            dismissLoading()
            Log.d("coroutinetestabcd", "我是工作三 done at ${Thread.currentThread().name}, show image $bitmap")
        }

        launch {
            //接收view click event
            for(view in channel) {
                Log.d("coroutinetestabcd", "我是Confirm Dialog start at ${Thread.currentThread().name}")
                val event = suspendContFun<Event>(Dispatchers.Main) { context, cont ->
                    showConfirmCont(cont)
                }.await()
                Log.d("coroutinetestabcd", "我是Confirm Dialog end at ${Thread.currentThread().name}, result ${event}")
            }
        }
    }

    private fun updateResult(result:String="") {
        Log.d("coroutinetestabcd", "我是工作三 updateResult at ${Thread.currentThread().name}, result is $result")
    }

    sealed class Event{
        object OK: Event()
        object Cancel: Event()
    }

    suspend fun <T> suspendContFun(context: CoroutineContext, block: (context: CoroutineContext, cont: Continuation<T>) -> Unit): Deferred<T>{
        return GlobalScope.async(context){
            suspendCoroutine { continuation: Continuation<T> ->
                block(context, continuation)
            }
        }
    }

    private fun showConfirmCont(continuation: Continuation<Event>) {
        with(AlertDialog.Builder(this)){
            setTitle("Question：")
            setMessage("Press Ok")
            setCancelable(true)
            setPositiveButton("是的"){ dialog, which ->
                dialog.dismiss()
                continuation.resume(Event.OK)
            }
            setNegativeButton("不要"){ dialog, which ->
                dialog.dismiss()
                continuation.resume(Event.Cancel)
            }
            create()
        }.show()
    }

    private fun showConfirm() {
        with(AlertDialog.Builder(this)){
            setTitle("Question：")
            setMessage("Press Ok")
            setCancelable(true)
            setPositiveButton("是的", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            setNegativeButton("不要", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            create()
        }.show()
    }


    private fun dismissLoading() {

    }

    private fun showLoading(){

    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    suspend fun 我是工作一(): String {
        delay(100)
        Log.d("coroutinetestabcd", "我是工作一 start at ${Thread.currentThread().name}")
        return "我是工作一"
    }

    suspend fun 我是工作二(): String {
        delay(100)
        Log.d("coroutinetestabcd", "我是工作二 start at ${Thread.currentThread().name}")
        return "我是工作二"
    }

    suspend fun 我是工作三(): Deferred<String> {
        Log.d("coroutinetestabcd", "我是工作三 start at ${Thread.currentThread().name}")
        return async(Dispatchers.IO) {
            Log.d("coroutinetestabcd", "我是工作三 load image at ${Thread.currentThread().name}")
            delay(1000)
            Log.d("coroutinetestabcd", "我是工作三 load image done at ${Thread.currentThread().name}")
            "我是工作三Bitmap"
        }
    }

}
