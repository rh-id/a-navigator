package m.co.rh.id.anavigator.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import m.co.rh.id.anavigator.StatefulView
import java.io.Serializable


class ExampleComposePage : StatefulView<Activity>() {
    /* NOTE : it can be troublesome setting up kotlin fields when save state is enabled.
    * as the language itself tries to avoid null as default initial value.
    * Troublesome especially when dealing with serializable fields where null value is a valid use case.
    * not to mention data class is not serializable so no data classes.
    */
    private lateinit var mModel: Model

    override fun initState(activity: Activity?) {
        super.initState(activity)
        mModel = Model(0, "ExampleComposePage")
    }

    override fun createView(activity: Activity?, container: ViewGroup?): View {
        return ComposeView(activity!! as Context).apply {
            setContent {
                MaterialTheme {
                    // In Compose world
                    MessageCard(mModel)
                }
            }
        }
    }
}

class Model(var count: Int, var body: String) : Serializable

@SuppressLint("UnrememberedMutableState")
@Composable
fun MessageCard(model: Model) {
    // it is fine not to use "remember" since the state is maintained in model
    // just make sure to update the model for new value
    val count = mutableStateOf(model.count)
    Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = count.value.toString())
        Text(text = model.body)
        Button(onClick = {
            count.value = count.value + 1
            // set back the value to the model to save/maintain state
            model.count = count.value
        }) {
            Text("Count")
        }
    }
}
