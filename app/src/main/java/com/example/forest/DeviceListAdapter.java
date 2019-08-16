package com.example.forest;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceListViewHolder> {
    private List<BluetoothDevice> data = new ArrayList<>();
    private OnItemClickListener listener;

    public DeviceListAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public boolean addDevice(BluetoothDevice device) {
        for (BluetoothDevice knownDevice: data) {
            if (knownDevice.equals(device)) {
                return false;
            }
        }
        data.add(device);
        return true;
    }

    @NonNull
    @Override
    public DeviceListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_row, parent, false);
        return new DeviceListViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceListViewHolder holder, int position) {
        holder.setDevice(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice device);
    }

    public static class DeviceListViewHolder extends RecyclerView.ViewHolder {
        public View view;
        private BluetoothDevice device;

        public void setDevice(BluetoothDevice device) {
            this.device = device;
            TextView v = view.findViewById(R.id.device_list_item);
            v.setText(device.getName());
        }

        public DeviceListViewHolder(View v, final OnItemClickListener listener) {
            super(v);
            view = v;
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemClick(device);
                    }
                }
            });
        }
    }
}