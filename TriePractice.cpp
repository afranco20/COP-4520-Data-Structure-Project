// Trie Practice C++

#include <string>
#define ALPHA_SIZE 26
using namespace std;

class Trie{
public:
	int numWords;
	Trie* letter[ALPHA_SIZE];
	Trie ()  {
		this->numWords = 0;
		for(int i = 0; i < ALPHA_SIZE; i++)
			letter[i] = nullptr;
	}

	void insertion(std::string str);
	Trie* deletion(Trie *root, std::string, int index);
	bool contains(std::string str);

};

void Trie::insertion(std::string str) {
	int len = str.length();
	Trie *temp = this;
	for(int i = 0; i < len; i++)
	{
		int currChar = str[i] - 'a';
		if(!temp->letter[currChar])
			temp->letter[currChar] = new Trie();
		temp = temp->letter[currChar];
	}
	temp->numWords++;
}

bool noChildren(Trie *root) {
	for(int i = 0; i < ALPHA_SIZE; i++)
		if(root->letter[i])
			return false;
	return true;
}

Trie* Trie::deletion(Trie *root, std::string str, int index) {
	if(!root)
		return nullptr;
	if(str.length() == index) {
		// Decrease value if contains word
		if(root->numWords != 0)
			root->numWords--;
		// Delete if not a prefix of another word
		if(noChildren(root)) {
			delete(root);
			root = nullptr;
		}
		return root;
	}
	int charVal = str[index] - 'a';
	root->letter[charVal] = deletion(root->letter[charVal], str, index + 1);

	if(noChildren(root) && root->numWords == 0) {
		delete(root);
		root = nullptr;
	}
	return root;
}

bool Trie::contains(std::string str) {
	if(this == nullptr)
		return false;
	int len = str.length();
	Trie *temp = this;
	for(int i = 0; i < len; i++)
	{
		int currChar = str[i] - 'a';
		if(!temp->letter[currChar])
			return false;
		temp = temp->letter[currChar];
	}
	if(temp->numWords < 1)
		return false;
	return true;
}

int main(void) {
	printf("Hello World\n");
	Trie* root = new Trie();
	root->insertion("hello");
	if(root->contains("hello"))
		printf("True\n");
}