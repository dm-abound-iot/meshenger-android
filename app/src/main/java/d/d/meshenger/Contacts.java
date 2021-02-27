package d.d.meshenger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Contacts {
    private List<Contact> contacts;

    public Contacts() {
        contacts = new ArrayList<>();
    }

    public List<Contact> getContactList() {
        return contacts;
    }

    public List<Contact> getContactListCopy() {
        return new ArrayList<>(contacts);
    }

    public void addContact(Contact contact) {
        int idx = findContact(contact.getPublicKey());
        if (idx >= 0) {
            // contact exists - replace
            contacts.set(idx, contact);
        } else {
            contacts.add(contact);
        }
    }

    public void deleteContact(byte[] publicKey) {
        int idx = findContact(publicKey);
        if (idx >= 0) {
            this.contacts.remove(idx);
        }
    }

    private int findContact(byte[] publicKey) {
        for (int i = 0; i < contacts.size(); i += 1) {
            if (Arrays.equals(contacts.get(i).getPublicKey(), publicKey)) {
                return i;
            }
        }
        return -1;
    }

    public Contact getContactByPublicKey(byte[] pubKey) {
        for (Contact contact : contacts) {
            if (Arrays.equals(contact.getPublicKey(), pubKey)) {
                return contact;
            }
        }
        return null;
    }

    Contact getContactByName(String name) {
        for (Contact contact : contacts) {
            if (contact.getName().equals(name)) {
                return contact;
            }
        }
        return null;
    }
/*
    void addContact(Contact contact) {
        contacts.add(contact);
        this.service.db.addContact(contact);
        saveDatabase();
        LocalBroadcastManager.getInstance(this.service).sendBroadcast(new Intent("refresh_contact_list"));
    }

    void deleteContact(byte[] pubKey) {
        this.service.db.deleteContact(pubKey);
        saveDatabase();
        LocalBroadcastManager.getInstance(this.service).sendBroadcast(new Intent("refresh_contact_list"));
    }

    void setContactState(byte[] publicKey, Contact.State state) {
        Contact contact = getContactByPublicKey(publicKey);
        if (contact != null && contact.getState() != state) {
            contact.setState(state);
            LocalBroadcastManager.getInstance(this.service).sendBroadcast(new Intent("refresh_contact_list"));
        }
    }
 */
}
