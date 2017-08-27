package me.temoa.base.adapter;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import me.temoa.base.adapter.listener.OnItemClickListener;
import me.temoa.base.adapter.listener.OnItemLongClickListener;
import me.temoa.base.adapter.listener.OnLoadMoreListener;

/**
 * Created by Lai
 * on 2017/8/27 17:33
 */

public abstract class BaseAdapter<T> extends RecyclerView.Adapter<ViewHolder> {

    protected static final int VIEW_TYPE_FOOTER = -1;

    // 是否向下滑动
    protected boolean isScrollDown;
    // 是否加载更多
    protected boolean isOpenLoadMore;
    // 是否正在加载
    protected boolean isLoading = false;

    protected Context mContext;
    protected LayoutInflater mLayoutInflater;
    protected List<T> mItems;

    protected OnLoadMoreListener mLoadMoreListener;
    protected OnItemClickListener<T> mItemClickListener;
    protected OnItemLongClickListener<T> mItemLongClickListener;

    public abstract int getViewType(T item, int position);

    public BaseAdapter(Context context, List<T> items) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mItems = items == null ? new ArrayList<T>() : items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View holderView = null;
        if (isOpenLoadMore && viewType == VIEW_TYPE_FOOTER) {
            holderView = mLayoutInflater.inflate(R.layout.item_footer_load, parent, false);
        }
        return new ViewHolder(holderView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (isOpenLoadMore && getItemViewType(position) == VIEW_TYPE_FOOTER) return;

        final int pos = holder.getAdapterPosition();
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mItemClickListener != null) {
                    mItemClickListener.onClick(view, mItems.get(pos), pos);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mItemLongClickListener != null) {
                    mItemLongClickListener.onLongClick(view, mItems.get(pos), pos);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return isOpenLoadMore ? (mItems.size() == 0 ? 0 : mItems.size() + 1) : mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (isOpenLoadMore && position == getItemCount() - 1) {
            return VIEW_TYPE_FOOTER;
        } else {
            return getViewType(mItems.get(position), position);
        }
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (getItemViewType(holder.getLayoutPosition()) == VIEW_TYPE_FOOTER) {
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            if (params != null && params instanceof StaggeredGridLayoutManager.LayoutParams) {
                StaggeredGridLayoutManager.LayoutParams p
                        = (StaggeredGridLayoutManager.LayoutParams) params;
                p.setFullSpan(true);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (getItemViewType(position) == VIEW_TYPE_FOOTER) {
                        return gridLayoutManager.getSpanCount();
                    }
                    return 1;
                }
            });
        }
        setLoadMoreMode(recyclerView, layoutManager);
    }

    private void setLoadMoreMode(RecyclerView recyclerView, final RecyclerView.LayoutManager layoutManager) {
        if (!isOpenLoadMore || mLoadMoreListener == null) {
            return;
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (isOpenLoadMore && isScrollDown && !isLoading
                            && findLastVisibleItemPosition(layoutManager) + 1 == getItemCount()) {

                        if (mLoadMoreListener != null) {
                            mLoadMoreListener.onLoadMore();
                            isLoading = true;
                        }

                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                isScrollDown = dy > 0;
            }
        });
    }

    private int findLastVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
            int max = lastVisibleItemPositions[0];
            for (int value : lastVisibleItemPositions) {
                if (value > max) {
                    max = value;
                }
            }
            return max;
        }
        return -1;
    }

    public void openLoadMore(boolean isOpenLoadMore) {
        this.isOpenLoadMore = isOpenLoadMore;
    }

    public void setLoadMoreListener(OnLoadMoreListener listener) {
        mLoadMoreListener = listener;
    }


    public void setItemClickListener(OnItemClickListener<T> listener) {
        mItemClickListener = listener;
    }

    public void setItemLongClickListener(OnItemLongClickListener<T> listener) {
        mItemLongClickListener = listener;
    }

    public void setLoadCompleted() {
        isLoading = false;
        notifyItemRemoved(getItemCount() - 1);
    }

    public void setNewData(List<T> newItems) {
        mItems = newItems;
        notifyItemRangeChanged(0, newItems.size());
    }

    public void addData(List<T> items) {
        int originalSize = mItems.size();
        mItems.addAll(items);
        notifyItemRangeInserted(originalSize, items.size());
    }
}