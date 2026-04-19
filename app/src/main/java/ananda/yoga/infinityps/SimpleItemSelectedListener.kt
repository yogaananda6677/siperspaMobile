package ananda.yoga.infinityps

import android.view.View
import android.widget.AdapterView

class SimpleItemSelectedListener(
    private val onSelected: () -> Unit
) : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        onSelected()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
}