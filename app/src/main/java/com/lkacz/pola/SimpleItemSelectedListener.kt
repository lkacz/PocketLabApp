// You can place this in the same file or a separate file if desired.
import android.view.View
import android.widget.AdapterView

class SimpleItemSelectedListener(
    private val onItemSelectedAction: (Int) -> Unit
) : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        onItemSelectedAction(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
