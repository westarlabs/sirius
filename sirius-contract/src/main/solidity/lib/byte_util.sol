pragma solidity ^0.5.1;

//byte operat lib
library ByteUtilLib {

    function equal(bytes memory one, bytes memory two) internal pure returns (bool) {
        if (!(one.length == two.length)) {
            return false;
        }
        for (uint i=0; i<one.length; i++) {
            if (!(one[i] == two[i])) {
	            return false;
            }
        }
        return true;
    }

    function bytesToBytes32(bytes memory b) internal pure returns (bytes32) {
        return bytesToBytes32(b, 0);
    }

    function bytesToBytes32(bytes memory b, uint offset) internal pure returns (bytes32) {
        bytes32 out;

        for (uint i = 0; i < 32; i++) {
            out |= bytes32(b[offset + i] & 0xFF) >> (i * 8);
        }
        return out;
    }

    function bytes32ToBytes(bytes32 data) internal pure returns (bytes memory) {
        bytes memory bs = new bytes(32);
        for (uint256 i; i < 32; i++) {
            bs[i] = data[i];
        }

        return bs;
    }

    /**
     * @dev RLP encodes a string.
     * @param self The string to encode.
     * @return The RLP encoded string in bytes.
     */
    function string2byte(string memory self) internal pure returns (bytes memory encoded) {
        return bytes(self);
    }

    function address2hash(address addr) internal pure returns (bytes32 hash) {
        return keccak256(address2byte(addr));
    }

    function pubkey2Address(bytes memory publicKey) internal pure returns(address addr){
        return address(uint(keccak256(publicKey)) & 0x000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF);
    }

    /**
     * @dev RLP encodes an address.
     * @param self The address to encode.
     * @return The RLP encoded address in bytes.
     */
    function address2byte(address self) internal pure returns (bytes memory encoded) {
        assembly {
            let m := mload(0x40)
            mstore(add(m, 20), xor(0x140000000000000000000000000000000000000000, self))
            mstore(0x40, add(m, 52))
            encoded := m
        }
    }

    function bytesToAddress(bytes memory b) internal pure returns (address) {
        uint result = 0;
        for (uint i=0; i<b.length;i++) {
            uint8 c = uint8(b[i]);
            if (c >= 48 && c <= 57) {
                result = result * 16 + (c - 48);
            }
            if(c >= 65 && c<= 90) {
                result = result * 16 + (c - 55);
            }
            if(c >= 97 && c<= 122) {
                result = result * 16 + (c - 87);
            }
        }
        return address(result);
    }

    /**
     * @dev RLP encodes a uint.
     * @param self The uint to encode.
     * @return The RLP encoded uint in bytes.
     */
    function uint2byte(uint self) internal pure returns (bytes  memory encoded) {
        return toBinary(self);
    }

    /**
     * @dev RLP encodes an int.
     * @param self The int to encode.
     * @return The RLP encoded int in bytes.
     */
    function int2byte(int self) internal pure returns (bytes  memory encoded) {
        return toBinary(uint(self));
    }

    function bool2byte(bool self) internal pure returns (bytes memory data) {
        data = new bytes(1);
        //TODO ensure false should use 0x80 or 0x0
        data[0] = (self ? bytes1(0x01) : bytes1(0x0));
        return data;
    }

    /**
     * @dev Encode integer in big endian binary form with no leading zeroes.
     * @notice TODO: This should be optimized with assembly to save gas costs.
     * @param _x The integer to encode.
     * @return RLP encoded bytes.
     */
    function toBinary(uint _x) private pure returns (bytes memory encoded) {
        bytes memory b = new bytes(32);
        assembly {
            mstore(add(b, 32), _x)
        }
        return b;
    }

    /**
     * @dev Copies a piece of memory to another location.
     * @notice From: https://github.com/Arachnid/solidity-stringutils/blob/master/src/strings.sol.
     * @param _dest Destination location.
     * @param _src Source location.
     * @param _len Length of memory to copy.
     */
    function memcpy(uint _dest, uint _src, uint _len) internal pure {
        uint dest = _dest;
        uint src = _src;
        uint len = _len;

        for(; len >= 32; len -= 32) {
            assembly {
                mstore(dest, mload(src))
            }
            dest += 32;
            src += 32;
        }

        uint mask = 256 ** (32 - len) - 1;
        assembly {
            let srcpart := and(mload(src), not(mask))
            let destpart := and(mload(dest), mask)
            mstore(dest, or(destpart, srcpart))
        }
    }

    function append(bytes memory first, bytes memory second) internal pure returns (bytes memory appended) {
        appended = new bytes(first.length + second.length);
        uint flattenedPtr;
        assembly { flattenedPtr := add(appended, 0x20) }

        uint firstPtr;
        assembly { firstPtr := add(first, 0x20)}

        memcpy(flattenedPtr, firstPtr, first.length);
        flattenedPtr += first.length;

        uint secondPtr;
        assembly { secondPtr := add(second, 0x20)}

        memcpy(flattenedPtr, secondPtr, second.length);
        flattenedPtr += second.length;

        return appended;
    }

    // Assumes that enough memory has been allocated to store in target.
    function _copyToBytes(uint btsPtr, uint len) internal pure returns(bytes memory) {
        // Exploiting the fact that 'tgt' was the last thing to be allocated,
        // we can write entire words, and just overwrite any excess.
        bytes memory tgt = new bytes(len);
        uint i = 0;
        uint words;
        uint rOffset;
        uint wOffset;
        assembly {
            words := div(add(len, 31), 32)
            rOffset := btsPtr
            wOffset := add(tgt, 0x20)
        }

        for(;i<words;i++) {
            assembly {
                let offset := mul(i, 0x20)
                mstore(add(wOffset, offset), mload(add(rOffset, offset)))
            }
        }

        assembly {
            mstore(add(tgt, add(0x20, mload(tgt))), 0)
        }

        return tgt;
    }
}

//public function
library RLPLib {

    uint constant DATA_SHORT_START = 0x80;
    uint constant DATA_LONG_START = 0xB8;

    struct RLPItem {
        uint _unsafe_memPtr;    // Pointer to the RLP-encoded bytes.
        uint _unsafe_length;    // Number of bytes. This is the full length of the string.
    }

    struct Iterator {
        RLPItem _unsafe_item;   // Item that's being iterated over.
        uint _unsafe_nextPtr;   // Position of the next item in the list.
    }

    /// @dev Check if the RLP item is data.
    /// @param self The RLP item.
    /// @return 'true' if the item is data.
    function isData(RLPLib.RLPItem memory self) internal pure returns (bool ret) {
        if (self._unsafe_length == 0)
            return false;
        uint memPtr = self._unsafe_memPtr;
        assembly {
            ret := lt(byte(0, mload(memPtr)), 0xC0)
        }
    }

    // Get start position and length of the data.
    function _decode(RLPLib.RLPItem memory self) internal pure returns (uint memPtr, uint len) {
        if(!isData(self))
            revert();
        uint b0;
        uint start = self._unsafe_memPtr;
        assembly {
            b0 := byte(0, mload(start))
        }
        if (b0 < DATA_SHORT_START) {
            memPtr = start;
            len = 1;
            return (memPtr, len);
        }
        if (b0 < DATA_LONG_START) {
            len = self._unsafe_length - 1;
            memPtr = start + 1;
        } else {
            uint bLen;
            assembly {
                bLen := sub(b0, 0xB7) // DATA_LONG_OFFSET
            }
            len = self._unsafe_length - 1 - bLen;
            memPtr = start + bLen + 1;
        }
        return (memPtr, len);
    }

    /// @dev Decode an RLPItem into bytes. This will not work if the
    /// RLPItem is a list.
    /// @param self The RLPItem.
    /// @return The decoded string.
    function toData(RLPLib.RLPItem memory self) internal pure returns (bytes memory bts) {
        if(!isData(self))
            revert();
        uint rStartPos;
        uint len;
        (rStartPos, len) = _decode(self);
        bts = ByteUtilLib._copyToBytes(rStartPos, len);
    }
}

library Base58Util {

    bytes constant ALPHABET = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';

    function base58String2Bytes(string memory base58String) internal pure returns (bytes memory) {
        bytes memory content = bytes(base58String);
        return toBase58(content, content.length);
    }

    function bytes32ToBase58(bytes32 data) internal pure returns(string memory) {
        bytes memory tmp = ByteUtilLib.bytes32ToBytes(data);
        return toString(toBase58(tmp, 32));
    }

    function toString(bytes memory bts) private pure returns(string memory) {
        return string(bts);
    }

    function toBase58(bytes memory source, uint len) private pure returns (bytes memory) {
        if (source.length == 0) return new bytes(0);
        uint8[] memory digits = new uint8[](len * 2);
        digits[0] = 0;
        uint8 digitlength = 1;
        for (uint i=0; i<source.length; ++i) {
            uint carry = uint8(source[i]);
            for (uint j=0; j<digitlength; ++j) {
                carry += uint(digits[j]) * 256;
                digits[j] = uint8(carry % 58);
                carry = carry / 58;
            }

            while (carry > 0) {
                digits[digitlength] = uint8(carry % 58);
                digitlength++;
                carry = carry / 58;
            }
        }
        return toAlphabet(reverse(truncate(digits, digitlength)));
    }

    function truncate(uint8[] memory array, uint8 length)  private pure returns (uint8[] memory) {
        uint8[] memory output = new uint8[](length);
        for (uint i=0; i<length; i++) {
            output[i] = array[i];
        }
        return output;
    }

    function reverse(uint8[] memory input) private pure returns (uint8[] memory) {
        uint8[] memory output = new uint8[](input.length);
        for (uint i=0; i<input.length; i++) {
            output[i] = input[input.length-1-i];
        }
        return output;
    }

    function toAlphabet(uint8[] memory indices) private pure returns (bytes memory) {
        bytes memory output = new bytes(indices.length);
        for (uint i=0; i<indices.length; i++) {
            output[i] = ALPHABET[indices[i]];
        }
        return output;
    }

    function concat(bytes memory byteArray, bytes memory byteArray2) private pure returns (bytes memory) {
        bytes memory returnArray = new bytes(byteArray.length + byteArray2.length);
        uint16 i = 0;
        for (; i < byteArray.length; i++) {
            returnArray[i] = byteArray[i];
        }
        for (i; i < (byteArray.length + byteArray2.length); i++) {
            returnArray[i] = byteArray2[i - byteArray.length];
        }
        return returnArray;
    }
}