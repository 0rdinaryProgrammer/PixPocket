package com.android.pixpocket

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class FavAdapter(private val context: Context, private val contacts: List<Contact>) : BaseAdapter() {

    override fun getCount(): Int = contacts.size

    override fun getItem(position: Int): Any = contacts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            // Inflate layout if there's no reusable view
            view = LayoutInflater.from(context).inflate(R.layout.list_itemfav, parent, false)
            holder = ViewHolder(view)
            view.tag = holder // Store holder in view tag
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        // Get contact data
        val contact = contacts[position]

        // Bind data to views
        holder.contactImage.setImageResource(contact.imageResId)
        holder.contactName.text = contact.name

        return view
    }

    // ViewHolder class to optimize view lookup
    private class ViewHolder(view: View) {
        val contactImage: ImageView = view.findViewById(R.id.fav_image)
        val contactName: TextView = view.findViewById(R.id.fav_name)
    }
}
