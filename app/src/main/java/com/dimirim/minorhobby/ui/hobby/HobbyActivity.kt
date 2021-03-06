package com.dimirim.minorhobby.ui.hobby

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dimirim.minorhobby.R
import com.dimirim.minorhobby.databinding.ActivityHobbyBinding
import com.dimirim.minorhobby.databinding.ItemPostLargeBinding
import com.dimirim.minorhobby.databinding.ItemToggleTagBinding
import com.dimirim.minorhobby.models.ToggleTag
import com.dimirim.minorhobby.ui.adapters.PostRecyclerAdapter
import com.dimirim.minorhobby.ui.hobby_write.HobbyWriteActivity
import com.dimirim.minorhobby.ui.post.PostActivity
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.android.synthetic.main.activity_hobby.*
import kotlinx.android.synthetic.main.layout_tag_search.view.*
import kotlinx.coroutines.launch
import slush.Slush

class HobbyActivity : AppCompatActivity() {
    private lateinit var viewModel: HobbyViewModel
    private lateinit var postAdapter: PostRecyclerAdapter<ItemPostLargeBinding>
    private var hobbyId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityHobbyBinding =
            DataBindingUtil.setContentView<ActivityHobbyBinding>(this, R.layout.activity_hobby)

        viewModel = ViewModelProvider(this).get(HobbyViewModel::class.java)
        binding.vm = viewModel

        hobbyId = intent.getStringExtra("hobbyId")
        hobbyNameTextView.text = intent.getStringExtra("hobbyName")
        writeBtn.setOnClickListener {
            val intent = Intent(this, HobbyWriteActivity::class.java)
            intent.putExtra("hobbyId", hobbyId)
            startActivityForResult(intent, 0)
        }

        lifecycleScope.launch {
            binding.item = viewModel.postList
            viewModel.loadPost(hobbyId)
        }

        postAdapter = PostRecyclerAdapter(
            object : com.dimirim.minorhobby.ui.adapters.OnItemClickListener {
                override fun onItemClick(position: kotlin.Int) {
                    val intent = Intent(this@HobbyActivity, PostActivity::class.java)
                    intent.putExtra("postId", viewModel.postList[position].id)
                    startActivityForResult(intent, 0)
                }
            },
            R.layout.item_post_large,
            viewModel.postList
        )

        postRecyclerView.adapter = postAdapter

        search.setOnClickListener {
            val searchText = searchEditText.text.toString()
            lifecycleScope.launch {
                viewModel.loadPostBySearchText(hobbyId, searchText)
            }
        }

        filter.setOnClickListener {
            showTagSearchDialog()
        }

        backBtn.setOnClickListener {
            finish()
        }
    }

    private fun showTagSearchDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_tag_search, hobbyRoot, false)

        AlertDialog.Builder(this)
            .setView(view)
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.apply) { _, _ ->
                lifecycleScope.launch {
                    viewModel.updateFilter(
                        view.containsAllSwitch.isChecked
                    )
                    viewModel.loadPostBySearchText(hobbyId, searchEditText.text.toString())
                }
            }
            .show()

        view.containsAllSwitch.isChecked = viewModel.containsAll

        Slush.SingleType<ToggleTag>()
            .setItems(viewModel.allTags)
            .setItemLayout(R.layout.item_toggle_tag)
            .setLayoutManager(FlexboxLayoutManager(this))
            .onBindData<ItemToggleTagBinding> { binding, toggleTag ->
                binding.lifecycleOwner = this
                binding.item = toggleTag
            }
            .onItemClick { view, i ->
                val isEnabled = viewModel.allTags[i].isEnabled
                isEnabled.value = !isEnabled.value!!
                lifecycleScope.launch {
                    viewModel.loadPostBySearchText(hobbyId, searchEditText.text.toString())
                }
            }
            .into(view.tagRecyclerView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        lifecycleScope.launch {
            viewModel.loadPost(hobbyId)
        }
    }
}
