pragma solidity ^0.5.1;

library ByteUtilLib {
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
}